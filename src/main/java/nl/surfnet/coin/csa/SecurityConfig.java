package nl.surfnet.coin.csa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

import nl.surfnet.coin.csa.filter.VootFilter;
import nl.surfnet.coin.csa.janus.Janus;
import nl.surfnet.coin.csa.service.IdentityProviderService;
import nl.surfnet.coin.csa.service.VootClient;
import nl.surfnet.coin.csa.shibboleth.RichUserDetailsService;
import nl.surfnet.coin.csa.shibboleth.ShibbolethPreAuthenticatedProcessingFilter;
import nl.surfnet.coin.csa.shibboleth.mock.MockShibbolethFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

  @Autowired
  private Environment environment;

  @Autowired
  private VootClient vootClient;

  @Value("${admin.distribution.channel.teamname}")
  private String adminDistributionTeam;

  @Autowired
  private IdentityProviderService identityProviderService;

  @Autowired
  private Janus janusClient;

  @Override
  public void configure(WebSecurity web) throws Exception {
    web
      .ignoring()
      .antMatchers("/api/**","/public/**", "/css/**", "/font/**", "/images/**", "/img/**", "/js/**", "/health")
    ;
  }
  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .addFilterBefore(
        new ShibbolethPreAuthenticatedProcessingFilter(authenticationManagerBean(), environment),
        AbstractPreAuthenticatedProcessingFilter.class
      )
      .addFilterAfter(new VootFilter(vootClient, adminDistributionTeam, environment), ShibbolethPreAuthenticatedProcessingFilter.class)
      .authorizeRequests()
      .antMatchers("/shopadmin/**").hasRole("DISTRIBUTION_CHANNEL_ADMIN")
      .anyRequest().authenticated();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    LOG.info("Configuring AuthenticationManager with a PreAuthenticatedAuthenticationProvider");
    PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
    authenticationProvider.setPreAuthenticatedUserDetailsService(new RichUserDetailsService(environment, identityProviderService, janusClient));
    auth.authenticationProvider(authenticationProvider);
  }

  @Bean
  @Override
  protected AuthenticationManager authenticationManager() throws Exception {
    return super.authenticationManager();
  }

  @Bean
  @Profile("dev")
  public FilterRegistrationBean mockShibbolethFilter() {
    FilterRegistrationBean shibFilter = new FilterRegistrationBean();
    shibFilter.setFilter(new MockShibbolethFilter());
    shibFilter.addUrlPatterns("/shopadmin/*");
    shibFilter.setOrder(1);
    return shibFilter;
  }
}

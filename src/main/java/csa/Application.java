package csa;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import javax.sql.DataSource;

import csa.service.*;
import csa.util.LicenseContactPersonService;
import org.apache.catalina.Container;
import org.apache.catalina.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.swift.common.soap.jira.JiraSoapService;
import org.swift.common.soap.jira.JiraSoapServiceServiceLocator;

import csa.janus.JanusRestClient;
import csa.service.impl.JiraClientMock;
import csa.service.impl.ServicesServiceImpl;
import csa.util.mail.MockEmailerImpl;
import csa.api.cache.ServicesCache;
import csa.dao.LmngIdentifierDao;
import csa.interceptor.AuthorityScopeInterceptor;
import csa.interceptor.MenuInterceptor;
import csa.janus.Janus;
import csa.service.impl.CompoundSPService;
import csa.service.impl.EmailServiceImpl;
import csa.service.impl.JiraClientImpl;
import csa.service.impl.LmngServiceImpl;
import csa.service.impl.LmngServiceMock;
import csa.service.impl.VootClientImpl;
import csa.service.impl.VootClientMock;
import csa.util.JanusRestClientMock;
import csa.util.SpringMvcConfiguration;
import csa.util.mail.Emailer;
import csa.util.mail.EmailerImpl;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class Application extends SpringBootServletInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);

  public static final String DEV_PROFILE_NAME = "dev";
  private static final String JIRA_SOAP_SERVICE_ENDPOINT = "/rpc/soap/jirasoapservice-v2";

  @Autowired
  private ResourceLoader resourceLoader;

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(Application.class);
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  @Autowired
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  @Autowired
  public VootClient vootClient(Environment environment, @Value("${voot.accessTokenUri}") String accessTokenUri,
                               @Value("${voot.clientId}") String clientId,
                               @Value("${voot.clientSecret}") String clientSecret,
                               @Value("${voot.scopes}") String scopes,
                               @Value("${voot.serviceUrl}") String serviceUrl) {
    if (environment.acceptsProfiles(DEV_PROFILE_NAME)) {
      LOG.debug("Using mock vootclient");
      return new VootClientMock();
    }
    return new VootClientImpl(accessTokenUri, clientId, clientSecret, scopes, serviceUrl);
  }

  @Bean
  public LocaleResolver localeResolver() {
    final CookieLocaleResolver localeResolver = new CookieLocaleResolver();
    localeResolver.setDefaultLocale(new Locale("en"));
    return localeResolver;
  }

  @Bean
  @Autowired
  public Janus janus(Environment environment, @Value("${janus.uri}") String uri, @Value("${janus.user}") String user, @Value("${janus.secret}") String secret) throws Exception {
    if (environment.acceptsProfiles(DEV_PROFILE_NAME)) {
      return new JanusRestClientMock();
    }
    return new JanusRestClient(new URI(uri), user, secret);
  }

  @Bean
  @Autowired
  public JiraClient jiraClient(Environment environment, @Value("${jiraBaseUrl}") String url,
                                 @Value("${jiraUsername") String username,
                                 @Value("${jiraPassword") String password, @Value("${jiraProjectKey") String projectKey) throws Exception{
    if (environment.acceptsProfiles(DEV_PROFILE_NAME)) {
      return new JiraClientMock();
    }
    @SuppressWarnings("serial")
    JiraSoapServiceServiceLocator jiraSoapServiceGetter = new JiraSoapServiceServiceLocator() {
      {
        setJirasoapserviceV2EndpointAddress(url + JIRA_SOAP_SERVICE_ENDPOINT);
        setMaintainSession(true);
      }
    };
    final JiraSoapService jiraSoapService = jiraSoapServiceGetter.getJirasoapserviceV2();
    return new JiraClientImpl(jiraSoapService, username, password, projectKey);
  }


  @Bean
  @Autowired
  public CrmService crmService(Environment environment, LmngIdentifierDao lmngIdentifierDao, @Value("${crmServiceClassEndpoint}") String endpoint) {
    if (environment.acceptsProfiles(DEV_PROFILE_NAME)) {
      return new LmngServiceMock();
    }
    return new LmngServiceImpl(lmngIdentifierDao, true, endpoint);
  }

  @Bean
  @Autowired
  public EmailService emailService(Environment environment, JavaMailSender mailSender, @Value("${coin-administrative-email}") String administrativeEmail) {
    Emailer emailer = environment.acceptsProfiles(DEV_PROFILE_NAME) ? new MockEmailerImpl() : new EmailerImpl(mailSender);
    return new EmailServiceImpl(administrativeEmail, emailer);
  }

  @Bean
  @Autowired
  public Emailer emailer(Environment environment, JavaMailSender mailSender) {
    if (environment.acceptsProfiles(DEV_PROFILE_NAME)) {
      return new MockEmailerImpl();
    } else {
      final EmailerImpl emailer = new EmailerImpl(mailSender);
      return emailer;
    }
  }


  @Bean
  @Autowired
  public WebMvcConfigurerAdapter webMvcConfigurerAdapter() {

    final LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
    localeChangeInterceptor.setParamName("lang");

    AuthorityScopeInterceptor authorityScopeInterceptor = new AuthorityScopeInterceptor();
    return new SpringMvcConfiguration(Arrays.asList(localeChangeInterceptor, authorityScopeInterceptor, new MenuInterceptor()));
  }

  @Bean
  @Autowired
  public ServicesCache servicesCache(CompoundSPService compoundSPService, CrmService crmService,
                                     @Value("${cacheMillisecondsStartupDelayTime}") long delay,
                                     @Value("${cacheMillisecondsServices}") long duration,
                                     @Value("${cacheMillisecondsCallDelay}") long callDelay,
                                     @Value("${static.baseurl}") String staticBaseUrl,
                                     @Value("${lmngDeepLinkBaseUrl}") String lmngDeepLinkBaseUrl,
                                     @Value("${public.api.lmng.guids}") String[] guids
                                     ) {
    return new ServicesCache(new ServicesServiceImpl(compoundSPService, crmService, staticBaseUrl, lmngDeepLinkBaseUrl, guids), delay, duration, callDelay);
  }

  @Bean
  @Autowired
  public LicenseContactPersonService licenseContactPersonService(
    @Value("${licenseContactPerson.config.path}") final String contentFileLocation) {
    return new LicenseContactPersonService(resourceLoader.getResource(contentFileLocation));
  }

  /**
   * Required because of https://github.com/spring-projects/spring-boot/issues/2825
   * As the issue says, probably can be removed as of Spring-Boot 1.3.0
   */
  @Bean
  public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
    return new EmbeddedServletContainerCustomizer() {

      @Override
      public void customize(ConfigurableEmbeddedServletContainer container) {
        if (container instanceof TomcatEmbeddedServletContainerFactory) {
          customizeTomcat((TomcatEmbeddedServletContainerFactory) container);
        }
      }

      private void customizeTomcat(TomcatEmbeddedServletContainerFactory tomcatFactory) {
        tomcatFactory.addContextCustomizers(context -> {
          Container jsp = context.findChild("jsp");
          if (jsp instanceof Wrapper) {
            ((Wrapper) jsp).addInitParameter("development", "false");
          }
        });
      }
    };
  }

  @Bean
  @Autowired
  public InternalResourceViewResolver viewResolver(@Value("${spring.view.prefix}") String prefix, @Value("${spring.view.suffix}") String suffix) {
    final InternalResourceViewResolver internalResourceViewResolver = new InternalResourceViewResolver();
    internalResourceViewResolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
    internalResourceViewResolver.setPrefix(prefix);
    internalResourceViewResolver.setSuffix(suffix);
    return internalResourceViewResolver;
  }
}

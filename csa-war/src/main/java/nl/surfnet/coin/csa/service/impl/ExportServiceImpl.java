package nl.surfnet.coin.csa.service.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import nl.surfnet.coin.csa.command.LmngServiceBinding;
import nl.surfnet.coin.csa.domain.CompoundServiceProvider;
import nl.surfnet.coin.csa.domain.FieldImage;
import nl.surfnet.coin.csa.domain.FieldString;
import nl.surfnet.coin.csa.domain.Screenshot;
import nl.surfnet.coin.csa.service.ExportService;

import org.springframework.stereotype.Service;

import au.com.bytecode.opencsv.CSVWriter;

@Service(value = "exportService")
public class ExportServiceImpl implements ExportService {

  @Override
  public String exportServiceBindingsCsv(List<LmngServiceBinding> bindings, String baseUrl) {
    StringWriter result = new StringWriter();
    CSVWriter csvWriter = new CSVWriter(result);
    
    //write CSV header
    csvWriter.writeNext(new String[]{"SP Entity", "CRM GUID", "available EndUser", "IDP Only Visible", "Field", "CRM value", "SurfConext value", "Distribution Channel Value", "Active Source"});
    
    for (LmngServiceBinding binding : bindings) {
      writeStringFields(csvWriter, binding);
      writeImageFields(csvWriter, binding, baseUrl);
    }
    
    
    // close the CSV writer
    try {
      csvWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    String csvResult = result.toString();
    
    return csvResult;
  }
  
  private void writeStringFields(final CSVWriter csvWriter, final LmngServiceBinding binding) {
    CompoundServiceProvider csp = binding.getCompoundServiceProvider();
    for (FieldString field : csp.getFields()) {
      String name = null == binding.getServiceProvider() ? binding.getCompoundServiceProvider().getServiceProviderEntityId() : binding.getServiceProvider().getName();
      String idpOnly = null == binding.getServiceProvider() ? "" : Boolean.toString(binding.getServiceProvider().isIdpVisibleOnly()); 
      csvWriter.writeNext(new String[]{
          name,
          binding.getLmngIdentifier(),
          Boolean.toString(csp.isAvailableForEndUser()),
          idpOnly,
          field.getKey().name(),
          csp.getLmngFieldValues().get(field.getKey()),
          csp.getSurfConextFieldValues().get(field.getKey()),
          csp.getDistributionFieldValues().get(field.getKey()),
          field.getSource().name()
        });
    }
  }
  
  private void writeImageFields(final CSVWriter csvWriter, final LmngServiceBinding binding, final String baseUrl) {
    CompoundServiceProvider csp = binding.getCompoundServiceProvider();
      for (FieldImage field : csp.getFieldImages()) {
        String name = null == binding.getServiceProvider() ? binding.getCompoundServiceProvider().getServiceProviderEntityId() : binding.getServiceProvider().getName();
        String idpOnly = null == binding.getServiceProvider() ? "" : Boolean.toString(binding.getServiceProvider().isIdpVisibleOnly());
          csvWriter.writeNext(new String[]{
              name,
              binding.getLmngIdentifier(),
              Boolean.toString(csp.isAvailableForEndUser()),
              idpOnly,
              field.getKey().name(),
              csp.getLmngFieldValues().get(field.getKey()),
              csp.getSurfConextFieldValues().get(field.getKey()),
              baseUrl + "/fieldimages/"+field.getId() +".img",
              field.getSource().name()
          });
      }
      for (Screenshot current : csp.getScreenShotsImages()) {
        String name = null == binding.getServiceProvider() ? binding.getCompoundServiceProvider().getServiceProviderEntityId() : binding.getServiceProvider().getName();
          csvWriter.writeNext(new String[]{
              name,
              binding.getLmngIdentifier(),
              Boolean.toString(csp.isAvailableForEndUser()),
              Boolean.toString(binding.getServiceProvider().isIdpVisibleOnly()),
              "_SCREENSHOT_",
              "",
              "",
              baseUrl + current.getFileUrl(),
              ""
          });
      }
  }
}

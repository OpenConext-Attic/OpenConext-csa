var app = app || {};

app.spServices = function () {

  var init = function () {
    var servicesTable = $('#sp_overview_table');

    if (servicesTable.length === 0) {
      return;
    }

    var performAjaxUpdate = function(elem, methodPart, newValue) {
      var $elm = $(elem);
      var tokencheck = $elm.parent("td").find("input[name='tokencheck']").val();
      var cspId = $elm.data('compound-service-provider-id');
      $.ajax(methodPart + "/" + cspId + "/" + newValue + ".shtml?tokencheck=" + tokencheck,
        {
          type: "PUT"
        })
        .done(function (data) {
          var $mess = $("<span>" + app.message.i18n('success.save') + "</span>");
          $elm.before($mess);
          $mess.fadeOut(1750);
        })
        .fail(function (data) {
          var $mess = $("<span>" + app.message.i18n('failed.save') + "</span>");
          $elm.before($mess);
        });
    };

    $('#sp_overview_table').find("input[type='checkbox'][name='availableForEndUser']").click(function () {
      performAjaxUpdate(this, "update-enduser-visible", $(this).is(':checked'));
    });

    $('#sp_overview_table').find("select[name='licenseStatus']").change(function () {
      performAjaxUpdate(this, "update-license-status", this.value);
    });

  };

  return {
    init:init
  }
}();

app.register(app.spServices);

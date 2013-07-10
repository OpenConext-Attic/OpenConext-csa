(function ($) {

  $.fn.fixedHeader = function () {

    resetWidth = function (widths, elems) {
      elems.each(function (index) {
        $(this).width(widths[index]);
      });

    }

    return this.each(function () {
      var $table = $(this);

      var $headerColumns = $table.find('thead').find('th');
      var widths = [];
      $headerColumns.each(function (index) {
        widths[index] = $(this).width();
      });

      $table.find('thead').css({ position: 'fixed',
        width: $table.width() });

      resetWidth(widths, $headerColumns);

      var $bodyColumns = $table.find('tbody > tr').first().find('td');

      resetWidth(widths, $bodyColumns);

    });
  };

})(jQuery);
Ext.define('AIDRFM.common.Header', {
    extend: 'Ext.container.Container',
    alias: 'widget.aidr-header',

    width: '100%',
    html: '<div class="headerWrapper">' +
        '<div class="header"><a href="http://aidr.qcri.org"><img src="' + BASE_URL + '/resources/img/AIDR/aidr_logo_240x90.png"></a></div>' +
        '</div>'
});
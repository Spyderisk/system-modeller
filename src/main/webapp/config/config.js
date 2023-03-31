module.exports = {
    'END_POINT': JSON.stringify("/system-modeller"),
    'API_END_POINT': JSON.stringify("/system-modeller"),
    'API_VERSION': "2.1",
    'APPLICATION_NAME': JSON.stringify("SPYDERISK"),
    /*
        ability to switch the software between standalone and embedded modes
        note: when embedded mode used (i.e. mode.embedded=true) no navigation panels will be shown (hence easier
        to embed the software into existing pilot UIs, frameworks etc.)
    */
    'EMBEDDED': false,

    /*
        a configuration switch that will allow to leave a navigation bar with software brand only i.e. software name
        as defined in spring.application.name (i.e. when mode.display.menu.brand.only=true everything else apart from
        software name will be removed from the navigation bar menu)
        note: if mode.embedded will be set to true then navigation menu will be entirely hidden and current
        setting will not have any effect
     */
    'BRAND_ONLY': false,
};

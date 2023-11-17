export function openDocumentation(e, link) {
    e.stopPropagation();
    window.open("/documentation/" + link, "system-modeller-docs");
}

export function openApiDocs(e) {
    e.stopPropagation();
    window.open("/system-modeller/swagger-ui.html", "openapi-docs");
}

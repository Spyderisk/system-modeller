export function openDocumentation(e, link) {
    e.stopPropagation();
    window.open("/documentation/" + link, "system-modeller-docs");
}

export function openApiDocs(e) {
    e.stopPropagation();
    window.open("/system-modeller/swagger-ui.html", "openapi-docs");
}

export function openAdaptorApiDocs(e) {
    e.stopPropagation();
    window.open("/system-modeller/adaptor/docs#/", "adaptor-openapi-docs");
}

export function reportIssue(e) {
    e.stopPropagation();
    window.open("https://github.com/Spyderisk/system-modeller/blob/dev/CONTRIBUTING.md#how-to-open-a-query-or-bug-report", "report-issue");
}

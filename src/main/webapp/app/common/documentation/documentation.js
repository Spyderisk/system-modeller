export function openDocumentation(e, link) {
    e.stopPropagation();
    window.open("/documentation/" + link, "system-modeller-docs", "noopener");
}

export function openDomainDoc(e, modelId, entity) {
    e.stopPropagation();
    let docUrl = "/system-modeller/models/" + modelId + "/docs?entity=" + encodeURIComponent(entity);
    window.open(docUrl, 
        "domain-model-docs", "noopener");
    }

export function openApiDocs(e) {
    e.stopPropagation();
    window.open("/system-modeller/swagger-ui.html", "openapi-docs", "noopener");
}

export function openAdaptorApiDocs(e) {
    e.stopPropagation();
    window.open("/system-modeller/adaptor/docs#/", "adaptor-openapi-docs", "noopener");
}

export function reportIssue(e) {
    e.stopPropagation();
    window.open("https://github.com/Spyderisk/system-modeller/blob/dev/CONTRIBUTING.md#how-to-open-a-query-or-bug-report", "report-issue", "noopener");
}

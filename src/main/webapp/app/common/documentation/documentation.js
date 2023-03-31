export function openDocumentation(e, link) {
    e.stopPropagation();
    window.open("/documentation/" + link, "system-modeller-docs");
}

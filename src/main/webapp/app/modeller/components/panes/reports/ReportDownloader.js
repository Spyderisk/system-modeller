import variables from '../../../../common/styles/vars.scss';

let darkBlue = "#337ab7";

let preHtml = `<html> <head> <style>

        html {
        font-family: sans-serif;
        }
        
        .report-content {
            cursor: auto;
            overflow: auto;
            width: 90%;
            margin-left: auto;
            margin-right: auto;
            margin-top: 30px;
            margin-bottom: 30px;
         }
     
        .heading {
          color: ${darkBlue};
          font-size: 2.2em;
          font-weight: bold;
        }
    
        thead {
          background-color: ${darkBlue};
          color: white;
        }
    
        table {
          table-layout:fixed;
          border-collapse: collapse;
        }
    
        table, th, td {
          min-width: 100%;
          border: 1px solid black;
        }
    
        th, td {
          padding: 10px;
        }
    
        th {
          text-align: center;
          font-weight: normal;
        }
    
        .col-1 { width: 10% }

        .col-2 { width: 30% }

        .col-3 { width: 8% }

        .col-4 { width: 30% }

        .col-5 { width: 8% }

        ·col-6 { width: 14% }
    
        .title {
            font-size: 1.4em;
            font-weight: bold;
            margin-top: 30px;
            margin-bottom: 10px;
        }
    
        .sub-title {
          font-weight: bold;
          margin-top: 20px;
          margin-bottom: 5px;
        }
    </style> </head> <body>`;

let postHtml = "</body></html>";


export function exportHTML(element) {
    element = element.replace(new RegExp("<input ", 'g'), "<input onClick='return false;' ");
    let html = preHtml + element + postHtml;

    var blob = new Blob(['\ufeff', html], {
        type: 'text/html'
    });

    saveFile(blob, html);
}

/*
 * This function is based on example code at:
 * https://www.codexworld.com/export-html-to-word-doc-docx-using-javascript/
 */
function saveFile(blob, fileString){
    // Specify link url
    let url = 'data:text/html;charset=utf-8,' + encodeURIComponent(fileString);

    // Specify file name based on current time and date
    let date = new Date();
    let filename = 'Exported_Report' + "_" +
        date.toISOString().slice(0, 19).replace("T", "_") + ".htm";

    // Create download link element
    let downloadLink = document.createElement("a");

    document.body.appendChild(downloadLink);

    if(navigator.msSaveOrOpenBlob ){
        navigator.msSaveOrOpenBlob(blob, filename);
    }else{
        // Create a link to the file
        downloadLink.href = url;

        // Setting the file name
        downloadLink.download = filename;

        //triggering the function
        downloadLink.click();
    }

    document.body.removeChild(downloadLink);
}

/*
 * This code is based on the Word Export plugin for jQuery:
 * https://www.jqueryscript.net/other/Export-Html-To-Word-Document-With-Images-Using-jQuery-Word-Export-Plugin.html
 */
/*
function wordExport(element, fileName) {
    fileName = typeof fileName !== 'undefined' ? fileName : "jQuery-Word-Export";
    var static_content = {
        mhtml: {
            top: "Mime-Version: 1.0\nContent-Base: " + location.href + "\nContent-Type: Multipart/related; boundary=\"NEXT.ITEM-BOUNDARY\";type=\"text/html\"\n\n--NEXT.ITEM-BOUNDARY\nContent-Type: text/html; charset=\"utf-8\"\nContent-Location: " + location.href + "\n\n<!DOCTYPE html>\n<html>\n_html_</html>",
            head: "<head>\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n<style>\n_styles_\n</style>\n</head>\n",
            body: "<body>_body_</body>"
        }
    };
    var options = {
        maxWidth: 624
    };
    // Clone selected element before manipulating it
    var markup = $(element).clone();

    // Remove hidden elements from the output
    markup.each(function() {
        var self = $(this);
        if (self.is(':hidden'))
            self.remove();
    });

    // Embed all images using Data URLs
    var images = Array();
    var img = markup.find('img');
    for (var i = 0; i < img.length; i++) {
        // Calculate dimensions of output image
        var w = Math.min(img[i].width, options.maxWidth);
        var h = img[i].height * (w / img[i].width);
        // Create canvas for converting image to data URL
        var canvas = document.createElement("CANVAS");
        canvas.width = w;
        canvas.height = h;
        // Draw image to canvas
        var context = canvas.getContext('2d');
        context.drawImage(img[i], 0, 0, w, h);
        // Get data URL encoding of image
        var uri = canvas.toDataURL("image/png");
        $(img[i]).attr("src", img[i].src);
        img[i].width = w;
        img[i].height = h;
        // Save encoded image to array
        images[i] = {
            type: uri.substring(uri.indexOf(":") + 1, uri.indexOf(";")),
            encoding: uri.substring(uri.indexOf(";") + 1, uri.indexOf(",")),
            location: $(img[i]).attr("src"),
            data: uri.substring(uri.indexOf(",") + 1)
        };
    }

    // Prepare bottom of mhtml file with image data
    var mhtmlBottom = "\n";
    for (var i = 0; i < images.length; i++) {
        mhtmlBottom += "--NEXT.ITEM-BOUNDARY\n";
        mhtmlBottom += "Content-Location: " + images[i].location + "\n";
        mhtmlBottom += "Content-Type: " + images[i].type + "\n";
        mhtmlBottom += "Content-Transfer-Encoding: " + images[i].encoding + "\n\n";
        mhtmlBottom += images[i].data + "\n\n";
    }
    mhtmlBottom += "--NEXT.ITEM-BOUNDARY--";

    //TODO: load css from included stylesheet
    var styles = "";

    // Aggregate parts of the file together
    var fileContent = static_content.mhtml.top.replace("_html_", static_content.mhtml.head.replace("_styles_", styles) + static_content.mhtml.body.replace("_body_", markup.html())) + mhtmlBottom;

    // Create a Blob with the file contents
    var blob = new Blob([fileContent], {
        type: "application/msword;charset=utf-8"
    });

    //saveAs(blob, fileName + ".doc");
    //KEM: Here we use the Export2DocSave function used earlier, as the saveAs function is not available
    Export2DocSave(blob, fileContent, fileName + ".doc");
};
*/


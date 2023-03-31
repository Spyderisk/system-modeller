let _ = require('lodash');


export function wordWrap(str, maxWidth, newLineStr) {
  let done = false; let letres = ''; let res = '';
  do {
    let found = false;
    // Inserts new line at first whitespace of the line
    for (let i = maxWidth - 1; i >= 0; i--) {
      if (testWhite(str.charAt(i))) {
        res = res + [str.slice(0, i), newLineStr].join('');
        str = str.slice(i + 1);
        found = true;
        break;
      }
    }
    // Inserts new line at maxWidth position, the word is too long to wrap
    if (!found) {
      res += [str.slice(0, maxWidth), newLineStr].join('');
      str = str.slice(maxWidth);
    }

    if (str.length < maxWidth)
      done = true;
  } while (!done);

  return res + str;
}

function testWhite(x) {
  let white = new RegExp(/^\s$/);
  return white.test(x.charAt(0));
}


export function nameWrap(name, width, adjustNameSizes) {
  if (!adjustNameSizes) {
      return name;
  } else if (width < 300) {
    return _.truncate(name, {
      'length': 4,
      'separator': ' '
    });
  } else if (width < 500) {
    return _.truncate(name, {
      'length': 10,
      'separator': ' '
    });
  } else if (width < 600) {
    return _.truncate(name, {
      'length': 18,
      'separator': ' '
    });
  } else {
    return name;
  }
}
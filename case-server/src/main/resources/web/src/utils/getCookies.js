exports.getCookie = function(name) {
  function getCookieVal(offset) {
    let endstr = document.cookie.indexOf(';', offset);
    if (endstr === -1) {
      endstr = document.cookie.length;
    }
    return decodeURI(document.cookie.substring(offset, endstr));
  }
  let arg = name + '=';
  let alen = arg.length;
  let clen = document.cookie.length;
  let i = 0;
  let j = 0;
  while (i < clen) {
    j = i + alen;
    if (document.cookie.substring(i, j) === arg) return getCookieVal(j);
    i = document.cookie.indexOf(' ', i) + 1;
    if (i === 0) break;
  }
  return null;
};
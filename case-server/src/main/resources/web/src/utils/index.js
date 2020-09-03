/* eslint-disable */
export default {
  getQueryString(name) {
    let reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
    let r = window.location.search.substr(1).match(reg);
    if (r != null) return unescape(r[2]);
    return null;
  },
  setcookie(name, value) {
    let Days = 30;
    let exp = new Date();
    exp.setTime(exp.getTime() + Days * 24 * 60 * 60 * 1000);
    document.cookie =
      name + '=' + escape(value) + ';expires=' + exp.toGMTString();
  },
};

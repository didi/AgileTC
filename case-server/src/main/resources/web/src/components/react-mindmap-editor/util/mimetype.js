const SPLITOR = '\uFEFF';
const MIMETYPE = {
  'application/km': '\uFFFF',
};
const SIGN = {
  '\uFEFF': 'SPLITOR',
  '\uFFFF': 'application/km',
};
const MimeType = () => {
  /**
   * 用于将一段纯文本封装成符合其数据格式的文本
   * @method process 			private
   * @param  {MIMETYPE} mimetype 数据格式
   * @param  {String} text     原始文本
   * @return {String}          符合该数据格式下的文本
   * @example
   * 			var str = "123";
   * 			str = process('application/km', str); // 返回的内容再经过MimeType判断会读取出其数据格式为application/km
   * 			process('text/plain', str); // 若接受到一个非纯文本信息，则会将其转换为新的数据格式
   */
  /**
   * 注册数据类型的标识
   * @method registMimeTypeProtocol  	public
   * @param  {String} type 数据类型
   * @param  {String} sign 标识
   */
  /**
   * 获取已注册数据类型的协议
   * @method getMimeTypeProtocol  	public
   * @param  {String} type 数据类型
   * @param  {String} text|undefiend  文本内容或不传入
   * @return {String|Function}
   * @example
   * 			text若不传入则直接返回对应数据格式的处理(process)方法
   * 			若传入文本则直接调用对应的process方法进行处理，此时返回处理后的内容
   * 			var m = new MimeType();
   * 			var kmprocess = m.getMimeTypeProtocol('application/km');
   * 			kmprocess("123") === m.getMimeTypeProtocol('application/km', "123");
   *
   */
};
MimeType.registMimeTypeProtocol = (type, sign) => {
  if (sign && SIGN[sign]) {
    throw new Error('sing has registed!');
  }
  if (type && !!MIMETYPE[type]) {
    throw new Error('mimetype has registed!');
  }
  SIGN[sign] = type;
  MIMETYPE[type] = sign;
};
MimeType.getMimeType = (sign) => {
  if (sign !== undefined) {
    return SIGN[sign] || null;
  }
  return MIMETYPE;
};
MimeType.getSpitor = () => {
  return SPLITOR;
};
MimeType.process = (mimetype, text) => {
  if (!MimeType.isPureText(text)) {
    var _mimetype = MimeType.whichMimeType(text);
    if (!_mimetype) {
      throw new Error('unknow mimetype!');
    }
    text = MimeType.getPureText(text);
  }
  if (mimetype === false) {
    return text;
  }
  return mimetype + SPLITOR + text;
};
MimeType.getMimeTypeProtocol = (type, text) => {
  var mimetype = MIMETYPE[type] || false;
  if (text === undefined) {
    return MimeType.process.bind(this, mimetype);
  }
  return MimeType.process(mimetype, text);
};
MimeType.isPureText = (text) => {
  return !~text.indexOf(MimeType.getSpitor());
};

MimeType.getPureText = (text) => {
  if (MimeType.isPureText(text)) {
    return text;
  }
  return text.split(MimeType.getSpitor())[1];
};

MimeType.whichMimeType = (text) => {
  if (MimeType.isPureText(text)) {
    return null;
  }
  return MimeType.getMimeType(text.split(MimeType.getSpitor())[0]);
};
export default MimeType;

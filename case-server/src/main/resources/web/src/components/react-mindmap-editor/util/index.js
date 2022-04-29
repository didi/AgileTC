import preview from './preview';
import editInput from './editInput';
import clipboardRuntime from './clipboard';

const getQueryString = (name, search = window.location.search) => {
  let reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)');
  let r = search.substr(1).match(reg);
  if (r !== null) return unescape(r[2]);
  return null;
};
const guid = () => {
  return (+new Date() * 1e6 + Math.floor(Math.random() * 1e6)).toString(36);
};
const getUsedResource = (nodes) => {
  let usedResource = [];
  for (let i = 0; i < nodes.length; i++) {
    const resource = nodes[i].getData('resource');
    if (resource) usedResource.push(...resource);
  }
  return [...new Set(usedResource)];
};
export { getQueryString, guid, preview, editInput, clipboardRuntime, getUsedResource };

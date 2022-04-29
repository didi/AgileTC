export default function preview(node, previewNode) {
  const icon = node.getRenderer('NoteIconRenderer').getRenderShape();
  const box = icon.getRenderBox('screen');
  const $previewer = previewNode;
  const $container = document.getElementsByClassName('kityminder-core-container');
  const $outerContainer = document.getElementsByClassName('kityminder-editor-container');

  $previewer.scrollTop = 0;

  let x = box.left - (document.documentElement.clientWidth - $container[0].clientWidth);
  let y = box.bottom + 8 - $outerContainer[0].getBoundingClientRect().top;

  $previewer.style.left = Math.round(x) + 'px';
  $previewer.style.top = Math.round(y) + 'px';
}

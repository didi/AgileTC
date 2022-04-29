export default function editInput(seletedNode, inputNode, positionOnly) {
  const $previewer = inputNode;
  const $container = document.getElementsByClassName('kityminder-core-container');

  let x = seletedNode.getRenderBox().cx - seletedNode.getRenderBox().width / 2;
  let y = seletedNode.getRenderBox().cy + $container[0].offsetTop - 8;

  if (positionOnly === undefined) {
    $previewer.style.left = Math.round(x) + 'px';
    $previewer.style.top = Math.round(y) + 'px';
  } else {
    x = x + seletedNode.getRenderBox().width / 2;
    return { x, y };
  }
}

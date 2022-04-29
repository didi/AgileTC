import React, { Component } from 'react';
import { Button } from 'antd';
import jsonDiff from 'fast-json-patch';
import './DoGroup.scss';

const MAX_HISTORY = 100;

let doDiffs = [];

class DoGroup extends Component {
  state = {
    undoDiffs: [],
    redoDiffs: [],
    patchLock: false,
    lastSnap: this.props.minder && this.props.minder.exportJson(),
  };
  componentDidMount() {
    let { minder } = this.props;
    minder.on('import', this.reset);
    // minder.on('patch', this.updateSelection);
  }
  reset = () => {
    this.setState({
      undoDiffs: [],
      redoDiffs: [],
      lastSnap: this.props.minder.exportJson(),
    });
  };

  makeUndoDiff = () => {
    const { minder } = this.props;
    let { undoDiffs, lastSnap } = this.state;
    
    const headSnap = minder.exportJson();
    console.log('----headsnap---', headSnap);
    const diff = jsonDiff.compare(headSnap, lastSnap);

    const doDiff = jsonDiff.compare(lastSnap, headSnap);
    if (diff.length) {
      if (diff.length === 1 && diff[0].path === '/base') {
        const undoTop = undoDiffs.pop()
        undoTop.push(diff[0]);
        undoDiffs.push(undoTop);
      } else {
        undoDiffs.push(diff);
        doDiffs.push(doDiff);
      }
      
      while (undoDiffs.length > MAX_HISTORY) {
        undoDiffs.shift();
        doDiffs.shift();
      }
      lastSnap = headSnap;
      this.setState({ undoDiffs, lastSnap });
      return true;
    }
  };
  makeRedoDiff = () => {
    const { minder } = this.props;
    let { lastSnap, redoDiffs } = this.state;
    let revertSnap = minder.exportJson();
    redoDiffs.push(jsonDiff.compare(revertSnap, lastSnap));
    lastSnap = revertSnap;
    this.setState({ redoDiffs, lastSnap });
  };
  // 撤销
  undo = () => {
    this.setState({ patchLock: true }, () => {
      const { minder } = this.props;
      let { undoDiffs } = this.state;
      const undoDiff = undoDiffs.pop();
      doDiffs.pop();
      if (undoDiff) {
        minder.applyPatches(undoDiff);
        this.makeRedoDiff();
      }
      this.setState({ patchLock: false });
    });
  };
  // 重做
  redo = () => {
    this.setState({ patchLock: true }, () => {
      const { minder } = this.props;
      let { redoDiffs } = this.state;
      const redoDiff = redoDiffs.pop();
      if (redoDiff) {
        minder.applyPatches(redoDiff);
        this.makeUndoDiff();
      }
      this.setState({ patchLock: false });
    });
  };
  getAndResetPatch = () => {
    const diffs = [...doDiffs];
    doDiffs = [];
    return diffs;
  };
  changed = () => {
    const { patchLock } = this.state;
    if (window.minderData) {
      if (patchLock) return;
      if (this.makeUndoDiff()) {
        this.setState({ redoDiffs: [] });
      }
    }
  };
  hasUndo = () => {
    const { undoDiffs } = this.state;
    return !!undoDiffs.length;
  };
  hasRedo = () => {
    const { redoDiffs } = this.state;
    return !!redoDiffs.length;
  };

  updateSelection = (e) => {
    const { patchLock } = this.state;
    const { minder } = this.props;
    if (!patchLock) return;
    const patch = e.patch;
    // eslint-disable-next-line default-case
    switch (patch.express) {
      case 'node.add':
        minder.select(patch.node.getChild(patch.index), true);
        break;
      case 'node.remove':
      case 'data.replace':
      case 'data.remove':
      case 'data.add':
        minder.select(patch.node, true);
        break;
    }
  };

  render() {
    const { isLock } = this.props;
    let hasUndo = this.hasUndo();
    let hasRedo = this.hasRedo();
    if (isLock) {
      hasUndo = false;
      hasRedo = false;
    }
    return (
      <div className="nodes-actions" style={{ width: 64 }}>
        <Button
          title="撤销 (Ctrl + Z)"
          type="link"
          icon="left-circle"
          size="small"
          onClick={this.undo}
          disabled={!hasUndo}
        >
          撤销
        </Button>
        <Button
          title="重做 (Ctrl + Y)"
          type="link"
          size="small"
          disabled={!hasRedo}
          icon="right-circle"
          onClick={this.redo}
        >
          重做
        </Button>
      </div>
    );
  }
}
export default DoGroup;

import React from 'react';
import Link from 'umi/link';
import config from '@/page/index/config';

export default class Button extends React.Component {
  render() {
    let linkTo = config.RouterPrefix + this.props.to;

    return (
      <Link {...this.props} to={linkTo}>
        {this.props.children}
      </Link>
    );
  }
}

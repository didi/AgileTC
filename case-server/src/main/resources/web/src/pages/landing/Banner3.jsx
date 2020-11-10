import React from 'react'
import { Button } from 'antd'
import QueueAnim from 'rc-queue-anim'
import Texty from 'rc-texty'
import 'rc-texty/assets/index.css'

class Banner extends React.PureComponent {
  render() {
    const { ...currentProps } = this.props
    const { dataSource } = currentProps
    delete currentProps.dataSource
    delete currentProps.isMobile
    const children = dataSource.textWrapper.children.map(item => {
      const { name, texty, ...$item } = item
      if (name.match('button')) {
        return (
          <Button type="primary" key={name} {...$item}>
            {item.children}
          </Button>
        )
      }

      return (
        <div key={name} {...$item}>
          {texty ? <Texty type="mask-bottom">{item.children}</Texty> : item.children}
        </div>
      )
    })
    return (
      <div {...currentProps} {...dataSource.wrapper}>
        <QueueAnim key="QueueAnim" type={['bottom', 'top']} delay={200} {...dataSource.textWrapper}>
          {children}
        </QueueAnim>
      </div>
    )
  }
}
export default Banner

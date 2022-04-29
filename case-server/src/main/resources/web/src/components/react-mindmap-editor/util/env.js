import Util from '../util'

// 默认开发
const { host } = window.location

// 环境判断;
if (window.PUBLIC_FLAG !== undefined && window.PUBLIC_FLAG) {
  let env = Util.getQueryString('debug')
  if (env === 'test' || process.env.NODE_ENV === 'development') {
  } else if (env === 'pre') {
    misBaseUrl = ''
  } else {
    misBaseUrl = ''
  }
} else {
  if (process.env.NODE_ENV === 'development') {
    misBaseUrl = ''
  }
}

export default misBaseUrl


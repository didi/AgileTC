import axios from 'axios'
import { notification } from 'antd'

/**
 * 一、功能：
 * 1. 统一拦截http错误请求码；
 * 2. 统一拦截业务错误代码；
 * 3. 统一设置请求前缀
 * |-- 每个 http 加前缀 baseURL = /api/v1，从配置文件中获取 apiPrefix

 * 
 * 二、引包：
 * |-- axios：http 请求工具库
 * |-- notification：Antd组件 > 处理错误响应码提示信息
 * |-- routerRedux：dva/router对象，用于路由跳转，错误响应码跳转相应页面
 * |-- store：dva中对象，使用里面的 dispatch 对象，用于触发路由跳转
 */
// const { NODE_ENV } = process.env
window.apiPrefix = '/api'
// 设置全局参数，如响应超时时间5min，请求前缀等。
axios.defaults.timeout = 1000 * 60 * 5
// axios.defaults.baseURL = window.apiPrefix
axios.defaults.withCredentials = true
// 状态码错误信息
const codeMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '用户没有权限（令牌、用户名、密码错误）。',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。',
}

// 添加一个请求拦截器，用于设置请求过渡状态
axios.interceptors.request.use(
  config => {
    config.baseURL = window.apiPrefix
    return config
  },
  error => {
    return Promise.reject(error)
  },
)

// 添加一个返回拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    // 即使出现异常，也要调用关闭方法，否则一直处于加载状态很奇怪
    return Promise.reject(error)
  },
)

export default function request(url, opt) {
  // 调用 axios api，统一拦截
  const options = {}
  options.method = opt !== undefined ? opt.method : 'get'
  if (opt) {
    if (opt.body) {
      options.data = typeof opt.body === 'string' ? JSON.parse(opt.body) : opt.body
    }

    // if (opt.headers) options.headers = { 'Content-Type': 'application/x-www-form-urlencoded' }
    if (opt.params !== undefined) {
      url += '?'
      for (let key in opt.params) {
        if (opt.params[key] !== undefined && opt.params[key] !== '') {
          url = url + key + '=' + opt.params[key] + '&'
        }
      }
      url = url.substring(0, url.length - 1)
    }
  }
  return axios({
    url,
    ...options,
  })
    .then(response => {
      // >>>>>>>>>>>>>> 请求成功 <<<<<<<<<<<<<<
      // console.log(`【${opt.method} ${opt.url}】请求成功，响应数据：`, response)

      // 打印业务错误提示
      // if (response.data && response.data.code != '0000') {
      //   message.error(response.data.message)
      // }
      // eslint-disable-next-line
      if (response.data.code === 401 || response.code === 401) {
        let loginPath = `${response.data.data.login_url}?app_id=${response.data.data.app_id}`
        let pagePath = encodeURIComponent(window.location.href)
        const redirectURL = `${loginPath}&version=1.0&jumpto=${pagePath}`
        window.location.href = redirectURL
        return Promise.reject(new Error('服务不可用，请联系管理员'))
      }
      // >>>>>>>>>>>>>> 当前未登录 <<<<<<<<<<<<<<
      if (response.data.code === 99993 || response.code === 99993) {
        const redirectURL = `/login`
        window.location.href = redirectURL
        return Promise.reject(new Error('服务不可用，请联系管理员'))
      }
      return { ...response.data }
    })
    .catch(error => {
      // >>>>>>>>>>>>>> 请求失败 <<<<<<<<<<<<<<
      // 请求配置发生的错误
      if (!error.response) {
        // eslint-disable-next-line
        return console.log('Error', error.message);
      }

      // 响应时状态码处理
      const status = error.response.status
      const errortext = codeMessage[status] || error.response.statusText

      notification.error({
        message: `请求错误 ${status}`,
        description: errortext,
      })

      return { code: status, message: errortext }
    })
}

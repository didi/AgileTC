// import utils from './utils';
// utils.setcookie('username', 'user');
export const dva = {
  config: {
    onError(err) {
      err.preventDefault()
      // eslint-disable-next-line
      console.error(err.message);
    },
  },
}

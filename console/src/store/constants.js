const themes = Object.freeze({
  SYSTEM: 'system',
  LIGHT: 'light',
  DARK: 'dark',
  HIGH_CONTRAST: 'high-contrast',
});

const themeNames = {
  system: 'System',
  light: 'Light',
  dark: 'Dark',
  'high-contrast': 'High Contrast',
};

let apiURL = import.meta.env.VITE_RMAC_BRIDGE_SERVER_URL;
if (apiURL) {
  apiURL = apiURL.replace('ws', 'http');
}
apiURL = `${apiURL || ''}/api/hosts`;

const themeName = (theme) => themeNames[theme];

const mutationKeys = Object.freeze({
  SET_THEME: 'SET_THEME',
  SET_SYSTEM_THEME: 'SET_SYSTEM_THEME',
  SET_CONNECTED: 'SET_CONNECTED',
  SET_PING_TIMER: 'SET_PING_TIMER',
  SET_STATUS_MSG: 'SET_STATUS_MSG',
  SET_HOSTS_HEALTH: 'SET_HOSTS_HEALTH',
  SET_HOSTS: 'SET_HOSTS_HEALTH',
  SET_HOST_CONFIG: 'SET_HOST_CONFIG',
  PUSH_NOTIFICATION: 'PUSH_NOTIFICATION',
  SET_READ_ALL: 'SET_READ_ALL',
});

const notificationTypes = Object.freeze({
  SUCCESS: 'SUCCESS',
  INFO: 'INFO',
  WARN: 'WARN',
  ERROR: 'ERROR',
});

const notifications = Object.freeze({
  ECONN_FAILED: {
    type: notificationTypes.ERROR,
    title: 'Connection Failed',
    desc: 'Connection to bridge server failed',
  },
  EFETCH_HOSTS_FAILED: {
    type: notificationTypes.ERROR,
    title: 'Could not fetch hosts',
    desc: 'Could not fetch hosts',
  },
  ICONN_DISCONNECTED: {
    type: notificationTypes.WARN,
    title: 'Disconnected',
    desc: 'Disconnected from bridge server',
  },
});

export { themes, themeName, mutationKeys, notificationTypes, notifications, apiURL };

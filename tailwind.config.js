module.exports = {
  purge: [],
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {
      colors: {
        'x-grey-dark': '#21242b', //focused line bg
        'x-grey': '#282c34', // editor bg
        'x-grey-light': '#5b6268', // comment
        'x-white': '#bbc2cf', // raw text
        'x-green': '#98be65',
        'x-yellow': '#eab168',
        'x-orange': '#da8548',
        'x-blue': '#51afef',
        'x-blue-dark': '#2257a0',
        'x-purple': '#a9a1e1',
        'x-pink': '#c678dd',
        'x-red': '#ff6c6b'
      }},
  },
  variants: {
    extend: {},
  },
  plugins: [],
  purge: [
    './resources/assets/js/*.js',
    './resources/assets/index.html',
  ],
}

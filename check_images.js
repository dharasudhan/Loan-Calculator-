const fs = require('fs');
const files = fs.readdirSync('app/src/test/screenshots');
files.forEach(f => {
  const stat = fs.statSync(`app/src/test/screenshots/${f}`);
  console.log(`${f}: ${stat.size} bytes`);
});

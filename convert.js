const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Ensure sharp is installed
try {
  require.resolve('jimp');
} catch (e) {
  console.log('Installing jimp...');
  execSync('npm install jimp@0.16.13', { stdio: 'inherit' });
}

const Jimp = require('jimp');

async function convertScreenshots() {
  const dir = 'app/src/test/screenshots';
  const files = fs.readdirSync(dir).filter(f => f.endsWith('.png'));

  for (const file of files) {
    const inputPath = path.join(dir, file);
    const outputPath = path.join(dir, file.replace('.png', '.jpg'));
    
    console.log(`Converting ${file} to JPEG...`);
    const image = await Jimp.read(inputPath);
    await image.quality(95).writeAsync(outputPath);
    console.log(`Output: ${outputPath}`);
    
    // Optionally delete the PNG
    // fs.unlinkSync(inputPath);
  }
}

convertScreenshots().catch(console.error);

/**
 * 生成本地头像脚本
 * 使用 DiceBear API 下载头像到本地
 * 生成 30 种风格 x 2 个种子 = 60 个头像
 */

import { createAvatar } from '@dicebear/core';
import * as funEmoji from '@dicebear/fun-emoji';
import * as avataaars from '@dicebear/avataaars';
import * as avataaarsNeutral from '@dicebear/avataaars-neutral';
import * as adventurer from '@dicebear/adventurer';
import * as adventurerNeutral from '@dicebear/adventurer-neutral';
import * as bigEars from '@dicebear/big-ears';
import * as bigEarsNeutral from '@dicebear/big-ears-neutral';
import * as bigSmile from '@dicebear/big-smile';
import * as bottts from '@dicebear/bottts';
import * as botttsNeutral from '@dicebear/bottts-neutral';
import * as croodles from '@dicebear/croodles';
import * as croodlesNeutral from '@dicebear/croodles-neutral';
import * as dylan from '@dicebear/dylan';
import * as glass from '@dicebear/glass';
import * as icons from '@dicebear/icons';
import * as identicon from '@dicebear/identicon';
import * as initials from '@dicebear/initials';
import * as lorelei from '@dicebear/lorelei';
import * as loreleiNeutral from '@dicebear/lorelei-neutral';
import * as micah from '@dicebear/micah';
import * as miniavs from '@dicebear/miniavs';
import * as notionists from '@dicebear/notionists';
import * as notionistsNeutral from '@dicebear/notionists-neutral';
import * as openPeeps from '@dicebear/open-peeps';
import * as personas from '@dicebear/personas';
import * as pixelArt from '@dicebear/pixel-art';
import * as pixelArtNeutral from '@dicebear/pixel-art-neutral';
import * as rings from '@dicebear/rings';
import * as shapes from '@dicebear/shapes';
import * as thumbs from '@dicebear/thumbs';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 头像风格配置
const avatarStyles = [
  { name: 'fun-emoji', collection: funEmoji },
  { name: 'avataaars', collection: avataaars },
  { name: 'avataaars-neutral', collection: avataaarsNeutral },
  { name: 'adventurer', collection: adventurer },
  { name: 'adventurer-neutral', collection: adventurerNeutral },
  { name: 'big-ears', collection: bigEars },
  { name: 'big-ears-neutral', collection: bigEarsNeutral },
  { name: 'big-smile', collection: bigSmile },
  { name: 'bottts', collection: bottts },
  { name: 'bottts-neutral', collection: botttsNeutral },
  { name: 'croodles', collection: croodles },
  { name: 'croodles-neutral', collection: croodlesNeutral },
  { name: 'dylan', collection: dylan },
  { name: 'glass', collection: glass },
  { name: 'icons', collection: icons },
  { name: 'identicon', collection: identicon },
  { name: 'initials', collection: initials },
  { name: 'lorelei', collection: lorelei },
  { name: 'lorelei-neutral', collection: loreleiNeutral },
  { name: 'micah', collection: micah },
  { name: 'miniavs', collection: miniavs },
  { name: 'notionists', collection: notionists },
  { name: 'notionists-neutral', collection: notionistsNeutral },
  { name: 'open-peeps', collection: openPeeps },
  { name: 'personas', collection: personas },
  { name: 'pixel-art', collection: pixelArt },
  { name: 'pixel-art-neutral', collection: pixelArtNeutral },
  { name: 'rings', collection: rings },
  { name: 'shapes', collection: shapes },
  { name: 'thumbs', collection: thumbs },
];

// 种子列表 - 使用两个种子生成变体
const seeds = ['default', 'variant'];

// 输出目录
const outputDir = path.join(__dirname, '../src/assets/avatar');

// 确保输出目录存在
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

// 生成头像
async function generateAvatars() {
  console.log('开始生成本地头像...');
  console.log(`目标目录: ${outputDir}\n`);

  let count = 0;

  for (const style of avatarStyles) {
    for (let i = 0; i < seeds.length; i++) {
      const seed = seeds[i];
      const suffix = i === 0 ? '' : '-2';
      const fileName = `${style.name}${suffix}`;

      try {
        const avatar = createAvatar(style.collection, {
          seed: seed,
          size: 200,
        });

        const svg = avatar.toString();
        const filePath = path.join(outputDir, `${fileName}.svg`);

        fs.writeFileSync(filePath, svg);
        console.log(`✓ 生成: ${fileName}.svg`);
        count++;
      } catch (error) {
        console.error(`✗ 失败: ${fileName} - ${error.message}`);
      }
    }
  }

  console.log(`\n头像生成完成！保存在: ${outputDir}`);
  console.log(`共生成 ${count} 个头像 (${avatarStyles.length} 种风格 x ${seeds.length} 个种子)`);
}

generateAvatars().catch(console.error);
import { FileSystemLayer, createMarkerScanner } from './io.js';
import path from 'path';
import fs from 'fs';

/**
 * 完整的测试流程：创建文件夹 -> 写入文件 -> 扫描 -> 更改 -> 提交 -> 删除
 */
async function testMarkerScanner() {
    console.log('=== 完整测试流程 ===\n');

    // 1. 创建测试文件夹
    const testDir = path.join(process.cwd(), '/script/test-marker-scanner');
    console.log(`[1] 创建测试文件夹: ${testDir}`);
    
    // 如果文件夹已存在，先删除
    if (fs.existsSync(testDir)) {
        fs.rmSync(testDir, { recursive: true, force: true });
    }
    fs.mkdirSync(testDir, { recursive: true });
    console.log('✓ 测试文件夹创建成功\n');

    // 2. 创建 FileSystemLayer 实例
    const fls = new FileSystemLayer();

    // 3. 写入测试文件（包含 marker）
    console.log('[2] 创建测试文件\n');
    
    const testFile1 = path.join(testDir, 'TestFile1.java');
    const testFile2 = path.join(testDir, 'TestFile2.java');
    
    const content1 = `public class TestFile1 {
    //@@init
    public void init() {
        System.out.println("Initial");
    }
    
    //@@process
    public void process() {
        System.out.println("Process");
    }
}`;

    const content2 = `public class TestFile2 {
    //@@setup
    private void setup() {
        // Setup code here
    }
    
    //@@run
    public void run() {
        // Run code here
    }
}`;

    fls.writeFile(testFile1, content1);
    fls.writeFile(testFile2, content2);
    console.log(`✓ 创建文件: ${testFile1}`);
    console.log(`✓ 创建文件: ${testFile2}\n`);

    // 4. 提交初始文件
    console.log('[3] 提交初始文件\n');
    const submitResult1 = await fls.submit();
    if (submitResult1.success > 0) {
        console.log(`✓ 成功提交 ${submitResult1.success} 个文件\n`);
    } else {
        console.error('✗ 提交失败\n');
        return;
    }

    // 5. 验证文件已创建
    console.log('[4] 验证文件已创建\n');
    if (fs.existsSync(testFile1) && fs.existsSync(testFile2)) {
        console.log('✓ 文件已存在于文件系统\n');
    } else {
        console.error('✗ 文件不存在\n');
        return;
    }

    // 6. 扫描测试文件夹
    console.log('[5] 扫描测试文件夹\n');
    console.log(`扫描路径: ${testDir}`);
    console.log(`文件列表:`);
    const files = fs.readdirSync(testDir);
    files.forEach(f => console.log(`  - ${f}`));
    console.log('');
    
    const scanner = createMarkerScanner({
        include: ['*.java', '**/*.java']
    }, fls);

    const scanResult = await scanner(testDir);
    if (scanResult.success) {
        console.log(`✓ 扫描成功！`);
        console.log(`  - 扫描文件数: ${scanResult.value.filesScanned}`);
        console.log(`  - 登记标记数: ${scanResult.value.markersRecorded}\n`);
    } else {
        console.error('✗ 扫描失败:', scanResult.error);
        return;
    }

    // 7. 显示登记的标记
    console.log('[6] 登记的标记信息\n');
    const markers = fls.markers;
    for (const [filePath, markerList] of Object.entries(markers)) {
        if (filePath.includes('test-marker-scanner')) {
            console.log(`文件: ${path.basename(filePath)}`);
            markerList.forEach((marker) => {
                console.log(`  - ${marker.id}: 行 ${marker.content[0]}-${marker.content[1]}`);
            });
        }
    }
    console.log('');

    // 8. 测试 getByMarker
    console.log('[7] 测试 getByMarker\n');
    const getResult = fls.getByMarker(testFile1, 'init');
    if (getResult.success) {
        console.log(`✓ getByMarker 成功`);
        console.log(`标记 'init' 的内容:\n${getResult.value}\n`);
    } else {
        console.log('✗ getByMarker 失败:', getResult.error);
    }

    // 9. 测试 setByMarker - 更改内容
    console.log('[8] 测试 setByMarker - 更改内容\n');
    const newInitContent = `    public void init() {
        System.out.println("Updated Initial");
        System.out.println("New line added");
    }`;
    
    const setResult = fls.setByMarker(testFile1, 'init', newInitContent);
    if (setResult.success) {
        console.log(`✓ setByMarker 成功`);
        console.log(`  新行号范围: ${setResult.value.newStart}-${setResult.value.newEnd}`);
        console.log(`  行数变化: ${setResult.value.lineDiff}\n`);
    } else {
        console.log('✗ setByMarker 失败:', setResult.error);
        return;
    }

    // 10. 验证更改后的内容
    console.log('[9] 验证更改后的内容\n');
    const getResult2 = fls.getByMarker(testFile1, 'init');
    if (getResult2.success) {
        console.log(`✓ 获取更新后的内容:`);
        console.log(getResult2.value);
        console.log('');
    }

    // 11. 提交更改
    console.log('[10] 提交更改\n');
    const submitResult2 = await fls.submit();
    if (submitResult2.success > 0) {
        console.log(`✓ 成功提交 ${submitResult2.success} 个文件\n`);
    } else {
        console.error('✗ 提交失败\n');
    }

    // 12. 验证文件已更新（从文件系统读取）
    console.log('[11] 验证文件系统已更新\n');
    const fileContent = fs.readFileSync(testFile1, 'utf8');
    if (fileContent.includes('Updated Initial')) {
        console.log('✓ 文件系统已正确更新\n');
    } else {
        console.log('✗ 文件系统未正确更新\n');
    }

    // 13. 测试删除文件
    console.log('[12] 测试删除文件\n');
    const deleteResult = fls.deleteFile(testFile2);
    if (deleteResult.success) {
        console.log(`✓ 文件已标记为删除\n`);
    } else {
        console.log('✗ 删除标记失败:', deleteResult.error);
    }

    // 14. 提交删除
    console.log('[13] 提交删除\n');
    const submitResult3 = await fls.submit();
    if (submitResult3.success > 0) {
        console.log(`✓ 成功提交 ${submitResult3.success} 个操作\n`);
    }

    // 15. 验证文件已删除
    console.log('[14] 验证文件已删除\n');
    if (!fs.existsSync(testFile2)) {
        console.log('✓ 文件已从文件系统删除\n');
    } else {
        console.log('✗ 文件仍然存在\n');
    }

    // 16. 清理测试文件夹
    console.log('[15] 清理测试文件夹\n');
    if (fs.existsSync(testDir)) {
        fs.rmSync(testDir, { recursive: true, force: true });
        console.log('✓ 测试文件夹已删除\n');
    }

    console.log('=== 测试完成 ===');
}

// // 运行测试
// testMarkerScanner().catch(error => {
//     console.error('测试失败:', error);
//     process.exit(1);
// });

let fls=new FileSystemLayer();
let result=await createMarkerScanner({
    include: ['*.java', '**/*.java']
}, fls)('script/test-marker-scanner');
console.log(result);
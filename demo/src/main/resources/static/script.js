let allData = [];
let filteredData = [];
let statisticsData = []; // Dùng để render bảng thống kê
let analysis2Data = {}; // Dùng để render bảng phân tích 2

// API Configuration
const API_BASE_URL = 'http://localhost:8081/api/logdevices';

// Hàm gọi API để lấy dữ liệu
async function fetchDataFromAPI(dateString) {
    try {
        const url = `${API_BASE_URL}/all?date=${dateString}`;
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error('Lỗi khi gọi API:', error);
        throw error;
    }
}

async function fetchStatistics() {
    try {
        // Lấy ngày từ datePicker và chuyển đổi format
        const selectedDate = document.getElementById('datePicker').value;
        let dateString;
        
        if (selectedDate) {
            const d = new Date(selectedDate);
            const dd = String(d.getDate()).padStart(2, '0');
            const mm = String(d.getMonth() + 1).padStart(2, '0');
            const yyyy = d.getFullYear();
            dateString = `${dd}${mm}${yyyy}`;
        } else {
            // Nếu chưa chọn ngày thì dùng hôm nay
            const today = new Date();
            const dd = String(today.getDate()).padStart(2, '0');
            const mm = String(today.getMonth() + 1).padStart(2, '0');
            const yyyy = today.getFullYear();
            dateString = `${dd}${mm}${yyyy}`;
        }
        
        const response = await fetch(`${API_BASE_URL}/stat?date=${dateString}`);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const data = await response.json();
        statisticsData = data.map(item => ({
            deviceCode: item.deviceCode,
            macHc: item.macHc,
            countStatus1: Number(item.countStatus1) || 0,
            countStatus2: Number(item.countStatus2) || 0,
            countTotal: Number(item.countTotal) || 0
        }));
        console.log("=== statisticsData ===", statisticsData);
    } catch (err) {
        console.error('Lỗi khi tải thống kê:', err);
    }
}

// Hàm gọi API phân tích 2
async function fetchAnalysis2(dateString) {
    try {
        const url = `${API_BASE_URL}/stat2?date=${dateString}`;
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        analysis2Data = await response.json();
        console.log("=== analysis2Data ===", analysis2Data);
    } catch (err) {
        console.error('Lỗi khi tải phân tích 2:', err);
        throw err;
    }
}

// Hàm load dữ liệu
async function loadData() {
    try {
        const tbody = document.getElementById('dataTableBody');
        const recordCount = document.getElementById('recordCount');
        tbody.innerHTML = '<tr><td colspan="15" class="no-data">⏳ Đang tải dữ liệu...</td></tr>'; // Tăng colspan từ 14 lên 15
        recordCount.textContent = 'Đang tải...';

        // Lấy ngày từ datePicker
        const input = document.getElementById('datePicker').value;
        let dateString;
        if (input) {
            const d = new Date(input);
            const dd = String(d.getDate()).padStart(2, '0');
            const mm = String(d.getMonth() + 1).padStart(2, '0');
            const yyyy = d.getFullYear();
            dateString = `${dd}${mm}${yyyy}`;
        } else {
            // nếu user chưa chọn thì mặc định hôm nay
            const today = new Date();
            const dd = String(today.getDate()).padStart(2, '0');
            const mm = String(today.getMonth() + 1).padStart(2, '0');
            const yyyy = today.getFullYear();
            dateString = `${dd}${mm}${yyyy}`;
        }

        allData = await fetchDataFromAPI(dateString);
        filteredData = [...allData];
        renderTable();
    } catch (error) {
        const tbody = document.getElementById('dataTableBody');
        const recordCount = document.getElementById('recordCount');
        tbody.innerHTML = '<tr><td colspan="15" class="no-data">❌ Không thể tải dữ liệu</td></tr>'; // Tăng colspan từ 14 lên 15
        recordCount.textContent = 'Lỗi tải dữ liệu';
        console.error(error);
    }
}


// Hàm format thời gian
function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN');
}

// Hàm format log data để hiển thị đẹp hơn
function formatLogData(logString) {
    if (!logString) return '';

    try {
        // Bóc lớp escape ngoài cùng
        let inner = JSON.parse(logString);

        // Chuyển đổi thành JSON hợp lệ: thêm " quanh key
        inner = inner.replace(/([{,])(\s*)([a-zA-Z0-9_]+)\s*:/g, '$1"$3":');

        // Thêm " cho value text (vd: hcReportLog, iveaapfrafdyjalh)
        inner = inner.replace(/:"?([a-zA-Z0-9_\-]+)"?(?=[,}])/g, ':"$1"');

        // Parse lại thành object chuẩn
        const parsed = JSON.parse(inner);

        return JSON.stringify(parsed, null, 4);
    } catch (e) {
        console.error("Format fail", e);
        return logString;
    }
}


function cleanAndParseLog(logString) {
    try {
        // Bước 1: bỏ escape ngoài cùng
        let inner = JSON.parse(logString); 
        
        // Bước 2: thêm dấu " cho keys và string values
        inner = inner
            .replace(/([{,])(\s*)([a-zA-Z0-9_]+)\s*:/g, '$1"$3":') // thêm " cho keys
            .replace(/:([a-zA-Z0-9_\-]+)/g, ':"$1"');              // thêm " cho string values đơn giản

        // Bước 3: parse JSON chuẩn
        return JSON.parse(inner);
    } catch (e) {
        console.error("Parse failed", e);
        return logString;
    }
}



// Hàm render bảng
function renderTable() {
    const tbody = document.getElementById('dataTableBody');
    const recordCount = document.getElementById('recordCount');
    
    if (filteredData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="15" class="no-data">Không tìm thấy dữ liệu</td></tr>'; // Tăng colspan từ 14 lên 15
        recordCount.textContent = 'Tổng: 0 bản ghi';
        return;
    }

    const rows = filteredData.map((item, index) => `
        <tr>
            <td class="col-stt">${index + 1}</td>
            <td class="col-id">${item.id || item._id || ''}</td>
            <td class="col-serial">${item.serial || ''}</td>
            <td class="col-device-code">${item.deviceCode || ''}</td>
            <td class="col-device-type">${item.deviceType || ''}</td>
            <td class="col-mac">${item.macHc || ''}</td>
            <td class="col-fw-version">${item.fwVersion || ''}</td>
            <td class="col-status status-${item.statusTestCase}">${item.statusTestCase || ''}</td>
            <td class="col-type">${item.type || ''}</td>
            <td class="col-cmd">${item.cmd || ''}</td>
            <td class="col-rqi">${item.rqi || ''}</td>
            <td class="col-log">
                <pre class="log-cell">${formatLogData(item.log) || ''}</pre>
            </td>
            <td class="col-time datetime">${item.createdTime ? formatDateTime(item.createdTime) : ''}</td>
            <td class="col-time datetime">${item.updatedTime ? formatDateTime(item.updatedTime) : ''}</td>
            <td class="col-class">${item._class || 'com.example.demo.Entity.LogDevice'}</td>
        </tr>
    `).join('');

    tbody.innerHTML = rows;
    recordCount.textContent = `Tổng: ${filteredData.length} bản ghi`;
}

// Hàm tìm kiếm
function searchTable(searchTerm) {
    const term = searchTerm.toLowerCase().trim();
    
    if (!term) {
        filteredData = [...allData];
    } else {
        filteredData = allData.filter(item => {
            return Object.values(item).some(value => 
                value && value.toString().toLowerCase().includes(term)
            );
        });
    }
    
    renderTable();
}

// Event listeners
document.getElementById('searchInput')?.addEventListener('input', (e) => {
    searchTable(e.target.value);
});

// Khởi tạo trang
document.addEventListener('DOMContentLoaded', () => {
    loadData();
});

// Thêm nút refresh để reload dữ liệu
function refreshData() {
    loadData();
}

// Hàm hiển thị thống kê
async function showStatistics() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const modal = document.getElementById('statisticsModal');
    modal.style.display = 'block';
    await fetchStatistics();
    generateStatisticsTable();
}

// Hàm đóng modal thống kê
function closeStatistics() {
    const modal = document.getElementById('statisticsModal');
    modal.style.display = 'none';
}

// Hàm hiển thị phân tích 2
async function showAnalysis2() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const modal = document.getElementById('analysis2Modal');
    modal.style.display = 'block';
    
    try {
        // Chuyển đổi date format từ yyyy-mm-dd sang ddmmyyyy
        const d = new Date(selectedDate);
        const dd = String(d.getDate()).padStart(2, '0');
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const yyyy = d.getFullYear();
        const dateString = `${dd}${mm}${yyyy}`;
        
        await fetchAnalysis2(dateString);
        generateAnalysis2Table();
    } catch (error) {
        const content = document.getElementById('analysis2Content');
        content.innerHTML = '<div class="error">❌ Không thể tải dữ liệu phân tích</div>';
        console.error('Lỗi phân tích 2:', error);
    }
}

// Hàm đóng modal phân tích 2
function closeAnalysis2() {
    const modal = document.getElementById('analysis2Modal');
    modal.style.display = 'none';
}

// Hàm tạo bảng phân tích 2
function generateAnalysis2Table() {
    const content = document.getElementById('analysis2Content');
    
    if (!analysis2Data || Object.keys(analysis2Data).length === 0) {
        content.innerHTML = '<div class="no-data">Không có dữ liệu để phân tích</div>';
        return;
    }
    
    // Lấy danh sách MAC addresses và log keys
    const macAddresses = Object.keys(analysis2Data).sort();
    const firstMac = macAddresses[0];
    const logKeys = Object.keys(analysis2Data[firstMac]).sort();
    
    // Tạo HTML table
    let tableHTML = `
        <table class="statistics-table">
            <thead>
                <tr>
                    <th class="mac-header">MAC Address</th>
    `;
    
    // Header cho log keys
    logKeys.forEach(key => {
        tableHTML += `<th class="log-key">${key}</th>`;
    });
    
    tableHTML += `
                </tr>
            </thead>
            <tbody>
    `;
    
    // Dữ liệu cho từng MAC address
    macAddresses.forEach(mac => {
        tableHTML += `<tr><td class="mac-header">${mac}</td>`;
        logKeys.forEach(key => {
            const count = analysis2Data[mac][key] || 0;
            tableHTML += `<td class="count-cell ${count > 0 ? 'has-data' : 'no-data'}">${count}</td>`;
        });
        tableHTML += `</tr>`;
    });
    
    // Dòng tổng
    tableHTML += `<tr class="total-row"><td class="mac-header">TOTAL</td>`;
    logKeys.forEach(key => {
        let total = 0;
        macAddresses.forEach(mac => {
            total += analysis2Data[mac][key] || 0;
        });
        tableHTML += `<td class="count-cell total-cell">${total}</td>`;
    });
    tableHTML += `</tr>`;
    
    tableHTML += `
            </tbody>
        </table>
    `;
    
    content.innerHTML = tableHTML;
}

// Hàm tạo bảng thống kê
function generateStatisticsTable() {
    const content = document.getElementById('statisticsContent');
    
    // Lấy danh sách unique deviceCodes và macAddresses
    const deviceCodes = [...new Set(statisticsData.map(item => item.deviceCode))].sort();
    const macAddresses = [...new Set(statisticsData.map(item => item.macHc))].sort();
    
    // Tạo matrix để tính tổng
    const matrix = {};
    const totals = {};
    
    // Khởi tạo matrix và totals
    macAddresses.forEach(mac => {
        matrix[mac] = {};
        deviceCodes.forEach(code => {
            matrix[mac][code] = { status1: 0, status2: 0, total: 0 };
        });
    });
    
    deviceCodes.forEach(code => {
        totals[code] = { status1: 0, status2: 0, total: 0 };
    });
    
    // Điền dữ liệu vào matrix
    statisticsData.forEach(item => {
        const mac = item.macHc;
        const code = item.deviceCode;
        matrix[mac][code].status1 = item.countStatus1;
        matrix[mac][code].status2 = item.countStatus2;
        matrix[mac][code].total = item.countTotal;
        
        // Cộng vào tổng
        totals[code].status1 += item.countStatus1;
        totals[code].status2 += item.countStatus2;
        totals[code].total += item.countTotal;
    });
    
    // Tạo HTML table
    let tableHTML = `
        <table class="statistics-table">
            <thead>
                <tr>
                    <th rowspan="2" class="mac-header">MAC Address</th>
    `;
    
    // Header cho device codes
    deviceCodes.forEach(code => {
        tableHTML += `<th colspan="3" class="device-group">${code}</th>`;
    });
    
    tableHTML += `
                </tr>
                <tr>
    `;
    
    // Sub-header cho Status1, Status2, Total
    deviceCodes.forEach(code => {
        tableHTML += `
            <th class="status1">S1</th>
            <th class="status2">S2</th>
            <th class="total">Total</th>
        `;
    });
    
    tableHTML += `
                </tr>
            </thead>
            <tbody>
    `;
    
    // Dữ liệu cho từng MAC address
    macAddresses.forEach(mac => {
        tableHTML += `<tr><td class="mac-header">${mac}</td>`;
        deviceCodes.forEach(code => {
            const data = matrix[mac][code];
            tableHTML += `
                <td class="status1">${data.status1}</td>
                <td class="status2">${data.status2}</td>
                <td class="total">${data.total}</td>
            `;
        });
        tableHTML += `</tr>`;
    });
    
    // Dòng tổng
    tableHTML += `<tr class="total-row"><td class="mac-header">TOTAL</td>`;
    deviceCodes.forEach(code => {
        const total = totals[code];
        tableHTML += `
            <td class="status1">${total.status1}</td>
            <td class="status2">${total.status2}</td>
            <td class="total">${total.total}</td>
        `;
    });
    tableHTML += `</tr>`;
    
    tableHTML += `
            </tbody>
        </table>
    `;
    
    content.innerHTML = tableHTML;
}

// Dropdown menu for export
function toggleExportMenu() {
    const menu = document.getElementById('exportMenu');
    menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
}

// Hide dropdown when clicking outside
document.addEventListener('click', function(event) {
    const dropdown = document.querySelector('.dropdown');
    if (!dropdown.contains(event.target)) {
        document.getElementById('exportMenu').style.display = 'none';
    }
});

// Export functions
async function exportData() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const d = new Date(selectedDate);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const dateString = `${dd}${mm}${yyyy}`;
    
    downloadFile(`${API_BASE_URL}/export/data?date=${dateString}`, `logdevice_data_${dateString}.xlsx`);
    document.getElementById('exportMenu').style.display = 'none';
}

async function exportStat1() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const d = new Date(selectedDate);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const dateString = `${dd}${mm}${yyyy}`;
    
    downloadFile(`${API_BASE_URL}/export/stat1?date=${dateString}`, `logdevice_stat1_${dateString}.xlsx`);
    document.getElementById('exportMenu').style.display = 'none';
}

async function exportStat2() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const d = new Date(selectedDate);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const dateString = `${dd}${mm}${yyyy}`;
    
    downloadFile(`${API_BASE_URL}/export/stat2?date=${dateString}`, `logdevice_stat2_${dateString}.xlsx`);
    document.getElementById('exportMenu').style.display = 'none';
}

async function exportAll() {
    const selectedDate = document.getElementById('datePicker').value;
    if (!selectedDate) {
        alert('Vui lòng chọn ngày!');
        return;
    }
    
    const d = new Date(selectedDate);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const dateString = `${dd}${mm}${yyyy}`;
    
    // Download all three files
    setTimeout(() => downloadFile(`${API_BASE_URL}/export/data?date=${dateString}`, `logdevice_data_${dateString}.xlsx`), 100);
    setTimeout(() => downloadFile(`${API_BASE_URL}/export/stat1?date=${dateString}`, `logdevice_stat1_${dateString}.xlsx`), 500);
    setTimeout(() => downloadFile(`${API_BASE_URL}/export/stat2?date=${dateString}`, `logdevice_stat2_${dateString}.xlsx`), 900);
    
    document.getElementById('exportMenu').style.display = 'none';
}

// Helper function to download file
function downloadFile(url, filename) {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
/* NovaRTC 定时任务调度平台前端逻辑（原生 JS，无外部依赖） */

const PAGE_SIZE = 10;

let jobPageNum = 1;
let editingJobId = null;
let logJobId = null;
let logPageNum = 1;

/* ================= 通用 ================= */

function escapeHtml(value) {
    if (value === null || value === undefined) {
        return '';
    }
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function showToast(message, success) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + (success ? 'success' : 'error');
    toast.style.display = 'block';
    setTimeout(() => {
        toast.style.display = 'none';
    }, 2500);
}

async function request(url, method, body) {
    const options = {
        method: method || 'GET',
        headers: {'Content-Type': 'application/json'}
    };
    if (body !== undefined) {
        options.body = JSON.stringify(body);
    }
    const response = await fetch(url, options);
    if (!response.ok) {
        throw new Error('请求失败: HTTP ' + response.status);
    }
    const result = await response.json();
    if (result.code !== 0) {
        throw new Error(result.msg || '操作失败');
    }
    return result.data;
}

/**
 * UTC ISO 时间转浏览器本地时间展示
 */
function formatTime(isoValue) {
    if (!isoValue) {
        return '-';
    }
    const date = new Date(isoValue);
    if (isNaN(date.getTime())) {
        return isoValue;
    }
    const pad = (n) => String(n).padStart(2, '0');
    return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
        + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds());
}

function renderPagination(containerId, pageNum, total, onPageChange) {
    const container = document.getElementById(containerId);
    const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
    container.innerHTML =
        '<button class="btn" ' + (pageNum <= 1 ? 'disabled' : '') + ' onclick="' + onPageChange + '(' + (pageNum - 1) + ')">上一页</button>'
        + '<span>第 ' + pageNum + ' / ' + totalPages + ' 页，共 ' + total + ' 条</span>'
        + '<button class="btn" ' + (pageNum >= totalPages ? 'disabled' : '') + ' onclick="' + onPageChange + '(' + (pageNum + 1) + ')">下一页</button>';
}

/* ================= 任务列表 ================= */

function reloadList() {
    jobPageNum = 1;
    refreshList();
}

async function refreshList() {
    const keyword = document.getElementById('keyword').value.trim();
    const url = '/api/jobs?pageNum=' + jobPageNum + '&pageSize=' + PAGE_SIZE
        + (keyword ? '&keyword=' + encodeURIComponent(keyword) : '');
    try {
        const page = await request(url);
        renderJobTable(page.records);
        renderPagination('jobPagination', jobPageNum, page.total, 'gotoJobPage');
    } catch (e) {
        showToast(e.message, false);
    }
}

function gotoJobPage(pageNum) {
    jobPageNum = pageNum;
    refreshList();
}

function scheduleText(job) {
    if (job.scheduleType === 'CRON') {
        return 'CRON: ' + (job.cronExpression || '-');
    }
    return '每 ' + (job.fixedRateSeconds || '-') + ' 秒';
}

function renderJobTable(jobs) {
    const tbody = document.getElementById('jobTableBody');
    if (!jobs || jobs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="empty-row">暂无任务</td></tr>';
        return;
    }
    tbody.innerHTML = jobs.map((job) => {
        const statusTag = job.status === 1
            ? '<span class="tag tag-success">启用</span>'
            : '<span class="tag tag-info">停用</span>';
        const concurrentTag = job.concurrentAllowed === 1
            ? '<span class="tag tag-warning">允许</span>'
            : '<span class="tag tag-info">禁止</span>';
        const toggleText = job.status === 1 ? '停用' : '启用';
        const timeoutText = (job.timeoutSeconds || 60) + ' 秒 / '
            + (job.timeoutRerun === 1 ? '补跑' : '不补跑');
        return '<tr>'
            + '<td>' + escapeHtml(job.jobName) + '</td>'
            + '<td><span class="cell-ellipsis" title="' + escapeHtml(job.invokeClass) + '">'
            + escapeHtml(job.invokeClass.split('.').pop()) + '#' + escapeHtml(job.invokeMethod) + '</span></td>'
            + '<td><span class="cell-ellipsis" title="' + escapeHtml(job.invokeParams) + '">'
            + escapeHtml(job.invokeParams || '-') + '</span></td>'
            + '<td>' + escapeHtml(scheduleText(job)) + '</td>'
            + '<td>' + escapeHtml(timeoutText) + '</td>'
            + '<td>' + concurrentTag + '</td>'
            + '<td>' + statusTag + '</td>'
            + '<td>' + formatTime(job.nextFireTime) + '</td>'
            + '<td>'
            + '<button class="btn-link" onclick="openJobForm(\'' + job.id + '\')">编辑</button>'
            + '<button class="btn-link" onclick="runJob(\'' + job.id + '\')">执行一次</button>'
            + '<button class="btn-link" onclick="toggleJob(\'' + job.id + '\')">' + toggleText + '</button>'
            + '<button class="btn-link" onclick="openLogModal(\'' + job.id + '\', \'' + escapeHtml(job.jobName) + '\')">日志</button>'
            + '<button class="btn-link danger" onclick="deleteJob(\'' + job.id + '\')">删除</button>'
            + '</td>'
            + '</tr>';
    }).join('');
}

/* ================= 任务表单 ================= */

function onScheduleTypeChange() {
    const isCron = document.getElementById('fScheduleType').value === 'CRON';
    document.getElementById('fCronExpression').style.display = isCron ? '' : 'none';
    document.getElementById('cronLabel').style.display = isCron ? '' : 'none';
    document.getElementById('fFixedRateSeconds').style.display = isCron ? 'none' : '';
    document.getElementById('fixedRateLabel').style.display = isCron ? 'none' : '';
}

function openJobForm(jobId) {
    editingJobId = jobId || null;
    document.getElementById('jobFormTitle').textContent = jobId ? '编辑任务' : '新增任务';
    document.getElementById('fJobName').value = '';
    document.getElementById('fInvokeClass').value = '';
    document.getElementById('fInvokeMethod').value = '';
    document.getElementById('fInvokeParams').value = '';
    document.getElementById('fScheduleType').value = 'CRON';
    document.getElementById('fCronExpression').value = '';
    document.getElementById('fFixedRateSeconds').value = '';
    document.getElementById('fTimeoutSeconds').value = '60';
    document.getElementById('fTimeoutRerun').checked = false;
    document.getElementById('fConcurrentAllowed').checked = false;
    document.getElementById('fStatus').checked = true;
    document.getElementById('fRemark').value = '';
    onScheduleTypeChange();

    if (jobId) {
        request('/api/jobs/' + jobId).then((job) => {
            document.getElementById('fJobName').value = job.jobName || '';
            document.getElementById('fInvokeClass').value = job.invokeClass || '';
            document.getElementById('fInvokeMethod').value = job.invokeMethod || '';
            document.getElementById('fInvokeParams').value = job.invokeParams || '';
            document.getElementById('fScheduleType').value = job.scheduleType || 'CRON';
            document.getElementById('fCronExpression').value = job.cronExpression || '';
            document.getElementById('fFixedRateSeconds').value = job.fixedRateSeconds || '';
            document.getElementById('fTimeoutSeconds').value = job.timeoutSeconds || 60;
            document.getElementById('fTimeoutRerun').checked = job.timeoutRerun === 1;
            document.getElementById('fConcurrentAllowed').checked = job.concurrentAllowed === 1;
            document.getElementById('fStatus').checked = job.status === 1;
            document.getElementById('fRemark').value = job.remark || '';
            onScheduleTypeChange();
        }).catch((e) => showToast(e.message, false));
    }
    document.getElementById('jobFormModal').classList.add('show');
}

function closeJobForm() {
    document.getElementById('jobFormModal').classList.remove('show');
}

async function submitJobForm() {
    const scheduleType = document.getElementById('fScheduleType').value;
    const payload = {
        jobName: document.getElementById('fJobName').value.trim(),
        invokeClass: document.getElementById('fInvokeClass').value.trim(),
        invokeMethod: document.getElementById('fInvokeMethod').value.trim(),
        invokeParams: document.getElementById('fInvokeParams').value.trim() || null,
        scheduleType: scheduleType,
        cronExpression: scheduleType === 'CRON' ? document.getElementById('fCronExpression').value.trim() : null,
        fixedRateSeconds: scheduleType === 'FIXED_RATE'
            ? Number(document.getElementById('fFixedRateSeconds').value) : null,
        timeoutSeconds: Number(document.getElementById('fTimeoutSeconds').value),
        timeoutRerun: document.getElementById('fTimeoutRerun').checked ? 1 : 0,
        concurrentAllowed: document.getElementById('fConcurrentAllowed').checked ? 1 : 0,
        status: document.getElementById('fStatus').checked ? 1 : 0,
        remark: document.getElementById('fRemark').value.trim() || null
    };
    try {
        if (editingJobId) {
            await request('/api/jobs/' + editingJobId, 'PUT', payload);
            showToast('任务已更新', true);
        } else {
            await request('/api/jobs', 'POST', payload);
            showToast('任务已创建', true);
        }
        closeJobForm();
        refreshList();
    } catch (e) {
        showToast(e.message, false);
    }
}

/* ================= 列表操作 ================= */

async function toggleJob(id) {
    try {
        await request('/api/jobs/' + id + '/toggle', 'POST');
        showToast('状态已切换', true);
        refreshList();
    } catch (e) {
        showToast(e.message, false);
    }
}

async function runJob(id) {
    if (!confirm('确认立即执行一次该任务？')) {
        return;
    }
    try {
        await request('/api/jobs/' + id + '/run', 'POST');
        showToast('已发送执行通知，可在调度日志中查看结果', true);
    } catch (e) {
        showToast(e.message, false);
    }
}

async function deleteJob(id) {
    if (!confirm('确认删除该任务？删除后不再调度，历史日志保留。')) {
        return;
    }
    try {
        await request('/api/jobs/' + id, 'DELETE');
        showToast('任务已删除', true);
        refreshList();
    } catch (e) {
        showToast(e.message, false);
    }
}

/* ================= 调度日志 ================= */

function openLogModal(jobId, jobName) {
    logJobId = jobId;
    logPageNum = 1;
    document.getElementById('logModalTitle').textContent = '调度日志 - ' + jobName;
    document.getElementById('logModal').classList.add('show');
    refreshLogs();
}

function closeLogModal() {
    document.getElementById('logModal').classList.remove('show');
}

function gotoLogPage(pageNum) {
    logPageNum = pageNum;
    refreshLogs();
}

function sendStatusTag(status) {
    return status === 1
        ? '<span class="tag tag-success">成功</span>'
        : '<span class="tag tag-danger">失败</span>';
}

function executeStatusTag(status) {
    if (status === 1) {
        return '<span class="tag tag-success">成功</span>';
    }
    if (status === 2) {
        return '<span class="tag tag-danger">失败</span>';
    }
    return '<span class="tag tag-primary">待回传</span>';
}

async function refreshLogs() {
    try {
        const page = await request('/api/jobs/' + logJobId + '/logs?pageNum=' + logPageNum + '&pageSize=' + PAGE_SIZE);
        renderLogTable(page.records);
        renderPagination('logPagination', logPageNum, page.total, 'gotoLogPage');
    } catch (e) {
        showToast(e.message, false);
    }
}

function renderLogTable(logs) {
    const tbody = document.getElementById('logTableBody');
    if (!logs || logs.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-row">暂无日志</td></tr>';
        return;
    }
    tbody.innerHTML = logs.map((log) => {
        const errorMsg = log.executeErrorMsg || log.sendErrorMsg || '';
        return '<tr>'
            + '<td style="white-space: nowrap;">' + formatTime(log.triggerTime) + '</td>'
            + '<td>' + (log.triggerType === 'MANUAL' ? '手动' : '定时') + '</td>'
            + '<td>' + sendStatusTag(log.sendStatus) + '</td>'
            + '<td>' + executeStatusTag(log.executeStatus) + '</td>'
            + '<td>' + (log.durationMs === null || log.durationMs === undefined ? '-' : log.durationMs) + '</td>'
            + '<td><span class="cell-ellipsis" title="' + escapeHtml(log.executeInstance) + '">'
            + escapeHtml(log.executeInstance || '-') + '</span></td>'
            + '<td><span class="cell-ellipsis" title="' + escapeHtml(errorMsg) + '">'
            + escapeHtml(errorMsg || '-') + '</span></td>'
            + '</tr>';
    }).join('');
}

/* ================= 初始化 ================= */

refreshList();
setInterval(refreshList, 30000);

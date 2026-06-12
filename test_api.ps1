$baseUrl = "http://localhost:8690"
$headers = @{}

Write-Host "=== 固定资产管理系统功能测试 ==="
Write-Host ""

# 1. 登录
Write-Host "1. 登录"
$loginBody = '{"username":"admin","password":"admin123"}'
$bytes = [System.Text.Encoding]::UTF8.GetBytes($loginBody)
$resp = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Body $bytes -ContentType "application/json" -UseBasicParsing
$loginData = $resp.Content | ConvertFrom-Json
$token = $loginData.token
$headers.Authorization = "Bearer $token"
Write-Host "  成功! 用户: $($loginData.user.username), 角色: $($loginData.user.role)"
Write-Host ""

# 2. 创建资产
Write-Host "2. 创建固定资产 - 数控车床"
$body1 = '{"assetName":"数控车床","category":"生产设备","specification":"CK6150","originalValue":128000.00,"purchaseDate":"2023-01-15","usefulLife":10,"residualValueRate":5.00,"location":"生产车间A区","department":"生产部","responsiblePerson":"王强"}'
$bytes1 = [System.Text.Encoding]::UTF8.GetBytes($body1)
$resp1 = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets" -Method Post -Body $bytes1 -Headers $headers -ContentType "application/json" -UseBasicParsing
$asset1 = $resp1.Content | ConvertFrom-Json
Write-Host "  成功! ID=$($asset1.id), Code=$($asset1.assetCode)"
$asset1Id = $asset1.id
Write-Host ""

# 3. 创建资产2
Write-Host "3. 创建固定资产 - 办公电脑"
$body2 = '{"assetName":"办公电脑","category":"电子设备","specification":"ThinkPad X1 Carbon","originalValue":12800.00,"purchaseDate":"2024-03-01","usefulLife":5,"residualValueRate":3.00,"location":"行政办公室","department":"行政部","responsiblePerson":"李娜"}'
$bytes2 = [System.Text.Encoding]::UTF8.GetBytes($body2)
$resp2 = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets" -Method Post -Body $bytes2 -Headers $headers -ContentType "application/json" -UseBasicParsing
$asset2 = $resp2.Content | ConvertFrom-Json
Write-Host "  成功! ID=$($asset2.id), Code=$($asset2.assetCode)"
$asset2Id = $asset2.id
Write-Host ""

# 4. 创建资产3
Write-Host "4. 创建固定资产 - 货运卡车"
$body3 = '{"assetName":"货运卡车","category":"运输车辆","specification":"解放J6P 6.8米","originalValue":256000.00,"purchaseDate":"2022-06-20","usefulLife":8,"residualValueRate":4.00,"location":"物流仓库","department":"物流部","responsiblePerson":"张伟"}'
$bytes3 = [System.Text.Encoding]::UTF8.GetBytes($body3)
$resp3 = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets" -Method Post -Body $bytes3 -Headers $headers -ContentType "application/json" -UseBasicParsing
$asset3 = $resp3.Content | ConvertFrom-Json
Write-Host "  成功! ID=$($asset3.id), Code=$($asset3.assetCode)"
Write-Host ""

# 5. 查询列表
Write-Host "5. 查询资产列表"
$listUrl = "$baseUrl/api/fixed-assets" + '?page=0&size=10'
$listResp = Invoke-WebRequest -Uri $listUrl -Headers $headers -UseBasicParsing
$listData = $listResp.Content | ConvertFrom-Json
Write-Host "  总数: $($listData.totalElements)"
foreach ($a in $listData.content) {
    Write-Host "    $($a.assetCode) - $($a.assetName) - $($a.assetStatus)"
}
Write-Host ""

# 6. 折旧计提
Write-Host "6. 月度折旧计提 (2026年6月)"
$deprBody = '{"year":2026,"month":6}'
$deprBytes = [System.Text.Encoding]::UTF8.GetBytes($deprBody)
$deprResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/depreciation/calculate" -Method Post -Body $deprBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$deprData = $deprResp.Content | ConvertFrom-Json
Write-Host "  计提资产数: $($deprData.processedCount)"
Write-Host "  本月折旧总额: $($deprData.totalDepreciation) 元"
Write-Host "  跳过资产数: $($deprData.skippedCount)"
Write-Host ""

# 7. 资产调拨
Write-Host "7. 资产调拨"
$transBody = '{"newDepartment":"生产二部","newLocation":"生产车间B区","newResponsiblePerson":"李明","transferDate":"2026-06-01","remark":"车间调整"}'
$transBytes = [System.Text.Encoding]::UTF8.GetBytes($transBody)
$transResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/$asset1Id/transfer" -Method Post -Body $transBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$transData = $transResp.Content | ConvertFrom-Json
Write-Host "  成功! $($transData.oldDepartment) -> $($transData.newDepartment)"
Write-Host ""

# 8. 状态变更
Write-Host "8. 资产状态变更 - 在用->维修中"
$statusBody = '{"newStatus":"维修中","reason":"定期保养"}'
$statusBytes = [System.Text.Encoding]::UTF8.GetBytes($statusBody)
$statusResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/$asset1Id/status-change" -Method Post -Body $statusBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$statusData = $statusResp.Content | ConvertFrom-Json
Write-Host "  成功! $($statusData.oldStatus) -> $($statusData.newStatus)"
Write-Host ""

# 9. 状态变更回来
Write-Host "9. 资产状态变更 - 维修中->在用"
$statusBody2 = '{"newStatus":"在用","reason":"保养完成"}'
$statusBytes2 = [System.Text.Encoding]::UTF8.GetBytes($statusBody2)
$statusResp2 = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/$asset1Id/status-change" -Method Post -Body $statusBytes2 -Headers $headers -ContentType "application/json" -UseBasicParsing
$statusData2 = $statusResp2.Content | ConvertFrom-Json
Write-Host "  成功! $($statusData2.oldStatus) -> $($statusData2.newStatus)"
Write-Host ""

# 10. 资产处置
Write-Host "10. 资产处置申请 - 办公电脑报废"
$disposalBody = '{"disposalMethod":"报废","disposalReason":"设备老化","estimatedResidualIncome":5000.00}'
$disposalBytes = [System.Text.Encoding]::UTF8.GetBytes($disposalBody)
$disposalResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/$asset2Id/disposal" -Method Post -Body $disposalBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$disposalData = $disposalResp.Content | ConvertFrom-Json
$disposalId = $disposalData.id
Write-Host "  成功! ID=$disposalId, 状态=$($disposalData.approvalStatus)"
Write-Host ""

# 11. 审批
Write-Host "11. 资产处置审批 - 同意"
$approveBody = '{"approved":true,"remark":"同意报废"}'
$approveBytes = [System.Text.Encoding]::UTF8.GetBytes($approveBody)
$approveResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/disposals/$disposalId/approve" -Method Post -Body $approveBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$approveData = $approveResp.Content | ConvertFrom-Json
Write-Host "  成功! 状态: $($approveData.approvalStatus)"
Write-Host ""

# 12. 处置完成
Write-Host "12. 资产处置完成"
$completeBody = '{"actualIncome":3000.00}'
$completeBytes = [System.Text.Encoding]::UTF8.GetBytes($completeBody)
$completeResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/disposals/$disposalId/complete" -Method Post -Body $completeBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$completeData = $completeResp.Content | ConvertFrom-Json
Write-Host "  成功! 实际收入: $($completeData.actualDisposalIncome), 处置损益: $($completeData.disposalGainLoss)"
Write-Host ""

# 13. 统计接口
Write-Host "13. 统计 - 资产总览"
$overviewResp = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/stats/overview" -Headers $headers -UseBasicParsing
$overviewData = $overviewResp.Content | ConvertFrom-Json
Write-Host "  资产总数: $($overviewData.totalCount)"
Write-Host "  总原值: $($overviewData.totalOriginalValue)"
Write-Host "  总净值: $($overviewData.totalNetBookValue)"
Write-Host "  使用率: $($overviewData.usageRate)%"
Write-Host ""

# 14. 幂等性测试
Write-Host "14. 幂等性测试 - 重复计提6月折旧"
$deprResp2 = Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/depreciation/calculate" -Method Post -Body $deprBytes -Headers $headers -ContentType "application/json" -UseBasicParsing
$deprData2 = $deprResp2.Content | ConvertFrom-Json
Write-Host "  计提资产数: $($deprData2.processedCount) (应为0)"
Write-Host "  幂等性验证: $(if ($deprData2.processedCount -eq 0) {'通过'} else {'失败'})"
Write-Host ""

# 15. 权限测试
Write-Host "15. 权限测试 - 普通用户计提折旧"
$loginBody2 = '{"username":"wangq","password":"123456"}'
$loginBytes2 = [System.Text.Encoding]::UTF8.GetBytes($loginBody2)
$respLogin2 = Invoke-WebRequest -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBytes2 -ContentType "application/json" -UseBasicParsing
$login2Data = $respLogin2.Content | ConvertFrom-Json
$headers2 = @{Authorization="Bearer $($login2Data.token)"}
try {
    Invoke-WebRequest -Uri "$baseUrl/api/fixed-assets/depreciation/calculate" -Method Post -Body $deprBytes -Headers $headers2 -ContentType "application/json" -UseBasicParsing | Out-Null
    Write-Host "  失败: 普通用户意外获得权限!"
} catch {
    $status = [int]$_.Exception.Response.StatusCode
    Write-Host "  HTTP $status $(if ($status -eq 403) {'(正确) - 权限控制正常'} else {'(错误)'})"
}
Write-Host ""

Write-Host "=== 核心功能测试完成! ==="

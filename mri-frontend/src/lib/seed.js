export const seedPatients = [
  { id: 1, patientNo: 'P20260621001', name: '张明', gender: '男', birthDate: '1980-01-01', phone: '138****0001' },
  { id: 2, patientNo: 'P20260621002', name: '李华', gender: '女', birthDate: '1974-09-18', phone: '139****0002' },
];

export const seedExams = [
  { id: 1, patientId: 1, examItem: '头颅MRI平扫', priority: '普通', status: '已完成' },
  { id: 2, patientId: 2, examItem: '腰椎MRI增强', priority: '加急', status: '待检查' },
];

export const seedStudies = [
  { id: 1, examOrderId: 1, studyInstanceUid: '1.2.840.113619.20260621.001', description: '头颅MRI平扫', status: '已归档' },
];

export const seedReports = [
  { id: 1, examOrderId: 1, studyId: 1, findings: '头颅MRI平扫示脑实质未见明显异常信号。', status: '草稿' },
];

export const seedManifest = {
  study: seedStudies[0],
  watermark: '医院MRI影像系统',
  downloadEnabled: true,
  series: [
    {
      seriesId: 1,
      seriesName: 'T1_AX',
      bodyPosition: '头部',
      files: [
        { id: 1, seriesId: 1, fileName: 'T1_AX_001.dcm', storagePath: 'storage/mri-images/T1_AX_001.dcm', checksum: 'demo-checksum-001' },
        { id: 2, seriesId: 1, fileName: 'T1_AX_002.dcm', storagePath: 'storage/mri-images/T1_AX_002.dcm', checksum: 'demo-checksum-002' },
      ],
    },
    {
      seriesId: 2,
      seriesName: 'T2_COR',
      bodyPosition: '头部',
      files: [
        { id: 3, seriesId: 2, fileName: 'T2_COR_001.dcm', storagePath: 'storage/mri-images/T2_COR_001.dcm', checksum: 'demo-checksum-003' },
      ],
    },
  ],
};

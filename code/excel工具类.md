#### 写了个Excel解析工具类

以后如果要使用的话可以来这里参考一下，使用了apache的poi和阿里的easyExcel。 刚开始单纯的使用apache的poi，但是后面被通知其他部门的人使用poi时会出现消耗内存过大的情况，出现了线上问题，所以要改为使用阿里的easyExcel。 因为是写完后才通知的OVO,所以把两种方式都整合在了一个工具类里面。为了兼容之前的代码（解析处理入库一堆代码）,所以解析的格式写死了。

```java
@Slf4j
public class ExcelUtil extends AnalysisEventListener<Data> {

    /**
     * .xls格式
     */
    private static final String TYPE_XLS = ".xls";
    /**
     * .xlsx格式
     */
    private static final String TYPE_XLSX = ".xlsx";
    /**
     * 消息集合，每条消息有
     * 两行 第一行为帖子id,第二行为标签id列表用,分割
     */
    private List<List<String>> res= new ArrayList<>();

    /**
     * 使用poi解析excel
     * @param in
     * @param fileName
     * @return
     * @throws Exception
     */
    public static  List<List<String>> getObjectListByExcelOnPoi(InputStream in, String fileName) throws Exception {
        List<List<String>> resList = new ArrayList<>();
        try {
            // 创建excel工作簿
            Workbook work = getWorkbook(in, fileName);
            if (null == work) {
                throw new Exception("创建Excel工作薄为空！");
            }
            //
            Sheet sheet = null;
            //代表某一行
            Row row = null;
            //代表某一列
            Cell cell = null;
            for (int currentSheet = 0; currentSheet < work.getNumberOfSheets(); currentSheet++) {
                sheet = work.getSheetAt(currentSheet);
                if(sheet == null) {
                    continue;
                }
                // 滤过第一行标题
                for (int currentRow = sheet.getFirstRowNum(); currentRow <= sheet.getLastRowNum(); currentRow++) {
                    //从第二行开始,拿到整行数据
                    row = sheet.getRow(currentRow);
                    //如果当前行不存在或
                    if (row == null || row.getFirstCellNum() == currentRow) {
                        continue;
                    }
                    //单独处理一行的所有列,放到集合中
                    List<String> oneRowData = new ArrayList<>();
                    for (int currentCel = row.getFirstCellNum(); currentCel < row.getLastCellNum(); currentCel++) {
                        cell = row.getCell(currentCel);
                        //把每个单元格视为字符串类型
                        cell.setCellType(CellType.STRING);
                        oneRowData.add(cell.getStringCellValue());
                    }
                    resList.add(oneRowData);
                }
            }
            work.close();
        } catch (Exception e) {
            log.error("excel解析出现异常",e);
        }
        return resList;
    }

    /**
     * poi判断文件格式
     * @param in
     * @param fileName
     * @return
     */
    private static Workbook getWorkbook(InputStream in, String fileName) throws Exception {
        Workbook book = null;
        String filetype = fileName.substring(fileName.lastIndexOf("."));
        if(TYPE_XLS.equals(filetype)) {
            book = new HSSFWorkbook(in);
        } else if (TYPE_XLSX.equals(filetype)) {
            book = new XSSFWorkbook(in);
        } else {
            throw new Exception("请上传excel文件！");
        }
        return book;
    }

    /**
     * easyExcel每条数据解析时调用
     * @param data
     * @param analysisContext
     */
    @Override
    public void invoke(Data data, AnalysisContext analysisContext) {
        List<String> oneRow = new ArrayList<>();
        oneRow.add(data.getPostId());
        oneRow.add(data.getTagsIdListString());
        res.add(oneRow);
    }

    /**
     * easyExcel解析完后的处理
     * @param analysisContext
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

    /**
     * 通过eastExcel返回结果
     * @return
     */
    public static List<List<String>> getObjectListByExcelOnEasyExcel(InputStream inputStream){
        ExcelUtil excelUtil = new ExcelUtil();
        EasyExcel.read(inputStream,Data.class,excelUtil).sheet().doRead();
        return excelUtil.res;
    }

}
```

里面的Data类就是个实体类，和excel中的一行对应 。我刚开始想把Data类写为静态内部类,发现工具不支持静态内部类，然后就写外面了。 看工具类的描述,是会自动关闭inputstream流的，所以我这里没关闭。
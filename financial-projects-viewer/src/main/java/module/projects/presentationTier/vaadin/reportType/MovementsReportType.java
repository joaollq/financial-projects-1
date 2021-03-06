package module.projects.presentationTier.vaadin.reportType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import module.projects.presentationTier.vaadin.reportType.components.ReportViewerComponent;
import module.projects.presentationTier.vaadin.reportType.components.TableSummaryComponent;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import pt.ist.bennu.core._development.PropertiesManager;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;

public abstract class MovementsReportType extends ProjectReportType {
    final ReportViewerComponent reportViewer;
    TableSummaryComponent tableSummary;

    public MovementsReportType(Map<String, String> args) {
        super(args);
        reportViewer = new ReportViewerComponent(getQuery(), getCustomFormatter());
        setColumnNames(reportViewer.getTable());
        addComponent(reportViewer);
    }

    @Override
    public CustomTableFormatter getCustomFormatter() {
        return new CustomTableFormatter() {
            @Override
            public void format(Table table) {
                table.addGeneratedColumn(getMessage("financialprojectsreports.movements.column.details"), new ColumnGenerator() {
                    @Override
                    public Object generateCell(Table source, Object itemId, Object columnId) {
                        Property paiIDMOV = source.getItem(itemId).getItemProperty("PAI_IDMOV");
                        Link detailsLink =
                                new Link(getMessage("financialprojectsreports.movements.column.details"), new ExternalResource(
                                        "#projectsService?reportType=" + getChildReportName() + "&unit=" + getProjectID()
                                                + "&PAI_IDMOV=" + paiIDMOV));
                        detailsLink.setTargetName("_blank");
                        //line.addItemProperty(columnId, new ObjectProperty<Link>(detailsLink));
                        return detailsLink;
                    }
                });
            }
        };

    }

    abstract protected String getChildReportName();

    @Override
    protected ReportViewerComponent getReportViewer() {
        return reportViewer;
    }

    protected void setTableSummaryReport(TableSummaryComponent table) {
        tableSummary = table;
        addComponent(tableSummary);
    }

    @Override
    public void write(HSSFSheet sheet, HSSFFont headersFont) {
        reportViewer.write(sheet, headersFont);
        tableSummary.write(sheet, headersFont);

        Table t = reportViewer.getTable();

        HashMap<String, ArrayList<ArrayList<Object>>> results = loadChildrenData();

        for (Object itemId : t.getItemIds()) {
            Item item = t.getItem(itemId);
            String parentID = item.getItemProperty("PAI_IDMOV").getValue().toString();

            int rowNum = sheet.getLastRowNum() + 2;
            HSSFRow row = sheet.createRow(rowNum++);
            HSSFCell cell = row.createCell(0);
            HSSFCellStyle style = sheet.getWorkbook().createCellStyle();
            style.setFont(headersFont);
            cell.setCellStyle(style);
            cell.setCellValue(getTypeName() + " Nº" + parentID);

            rowNum = reportViewer.writeHeader(sheet, headersFont);
            row = sheet.createRow(rowNum++);
            int i = 0;
            for (Object propertyId : item.getItemPropertyIds()) {
                Property p = item.getItemProperty(propertyId);
                cell = row.createCell(i++);
                if (p.getValue() instanceof BigDecimal) {
                    BigDecimal number = (BigDecimal) p.getValue();
                    if (!propertyId.toString().equals("Rubrica")) {
                        number = number.setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                    String englishFormula = "VALUE(\"" + number.toString() + "\")";
                    String portugueseFormula = "VALUE(\"" + number.toString().replace(".", ",") + "\")";
                    cell.setCellFormula("IF(ISERROR(" + portugueseFormula + "), " + englishFormula + ", " + portugueseFormula
                            + ")");

                } else {
                    cell.setCellValue(p.getValue().toString());
                }
            }

            rowNum++;
            cell = sheet.createRow(rowNum++).createCell(0);
            cell.setCellValue(getChildTypeName());
            cell.setCellStyle(style);

            //write children data
            int cellCount = 0;
            row = sheet.createRow(rowNum++);
            for (String s : getChildQueryColumnsPresentationNames()) {
                cell = row.createCell(cellCount++);
                cell.setCellStyle(style);
                cell.setCellValue(s);
            }
            for (ArrayList<Object> entry : results.get(parentID)) {
                row = sheet.createRow(rowNum++);
                cellCount = 0;
                for (Object s : entry) {
                    cell = row.createCell(cellCount++);
                    if (s != null) {
                        if (s instanceof BigDecimal) {
                            String englishFormula = "VALUE(\"" + s.toString() + "\")";
                            String portugueseFormula = "VALUE(\"" + s.toString().replace(".", ",") + "\")";
                            cell.setCellFormula("IF(ISERROR(" + portugueseFormula + "), " + englishFormula + ", "
                                    + portugueseFormula + ")");
                        } else {
                            cell.setCellValue(s.toString());
                        }
                    }
                }
            }
        }

        sheet.createRow(sheet.getLastRowNum() + 2).createCell(0)
                .setCellValue(getMessage("financialprojectsreports.expensescalculationwarning"));
    }

    private HashMap<String, ArrayList<ArrayList<Object>>> loadChildrenData() {
        HashMap<String, ArrayList<ArrayList<Object>>> results = new HashMap<>();
        String query = "select distinct";
        //Add columns names to query
        query += "\"PAI_IDMOV\", ";

        List<String> queryColumns = getChildQueryColumns();
        for (int i = 0; i < queryColumns.size() - 1; i++) {
            query += queryColumns.get(i) + ", ";
        }
        query += queryColumns.get(queryColumns.size() - 1) + " ";

        //Add table name and where clause
        query += "from " + getChildQueryTableName() + " where \"PAI_IDPROJ\"='" + getProjectCode() + "'";
        query += "order by " + getOrderColumn();

        //Maps PAI_IDMOV to list of children. Each children is represented as a list of attributes 

        try {
            final String propPrefix = "db.mgp" + ReportViewerComponent.getHostPropertyPart();

            Connection con =
                    DriverManager.getConnection(ReportViewerComponent.getAlias(propPrefix),
                            PropertiesManager.getProperty(propPrefix + ".user"),
                            PropertiesManager.getProperty(propPrefix + ".pass"));

            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            //populate data structure with query result

            while (rs.next()) {
                String parentId = rs.getString("PAI_IDMOV");
                ArrayList<ArrayList<Object>> parentEntry = results.get(parentId);
                if (parentEntry == null) {
                    parentEntry = new ArrayList<ArrayList<Object>>();
                    results.put(parentId, parentEntry);
                }
                ArrayList<Object> currentdata = new ArrayList<Object>();
                parentEntry.add(currentdata);

                for (String columnName : getChildResultColumns()) {
                    currentdata.add(rs.getObject(columnName));
                }
            }
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    abstract protected String getOrderColumn();

    abstract protected List<String> getChildQueryColumnsPresentationNames();

    abstract protected List<String> getChildQueryColumns();

    abstract protected String getChildQueryTableName();

    abstract protected List<String> getChildResultColumns();

    @Override
    public TableSummaryComponent getSummary() {
        return tableSummary;
    }

    public abstract void setColumnNames(Table table);

    public abstract String getTypeName();

    public abstract String getChildTypeName();

}

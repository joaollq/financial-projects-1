package module.projects.presentationTier.vaadin.reportType;

import java.util.Map;

import module.projects.presentationTier.vaadin.reportType.components.ReportViewerComponent;
import module.projects.presentationTier.vaadin.reportType.components.TableSummaryComponent;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;

import com.vaadin.ui.Table;

public abstract class MovementsDetailsReportType extends ProjectReportType {
    String parentID;
    ReportViewerComponent reportViewer;
    TableSummaryComponent tableSummary;

    public MovementsDetailsReportType(Map<String, String> args) {

        super(args);
        parentID = args.get("PAI_IDMOV");
        reportViewer = new ReportViewerComponent(getQuery(), getCustomFormatter());
        setColumnNames(reportViewer.getTable());
        addComponent(reportViewer);

    }

    protected String getParentId() {
        return parentID;
    }

    protected void setTableSummaryReport(TableSummaryComponent table) {
        tableSummary = table;
        addComponent(tableSummary);
    }

    @Override
    protected ReportViewerComponent getReportViewer() {
        return reportViewer;
    }

    @Override
    public void write(HSSFSheet sheet, HSSFFont headersFont) {

        reportViewer.write(sheet, headersFont);
        //tableSummary.write(sheet, headersFont);
    }

    public abstract void setColumnNames(Table table);

    @Override
    public boolean isToExport() {
        return false;
    }
}

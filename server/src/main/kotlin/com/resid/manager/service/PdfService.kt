package com.resid.manager.service

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.resid.manager.dto.ElectricityStatementDto
import java.io.ByteArrayOutputStream
import java.awt.Color

object PdfService {

    fun generateEcoPrintPdf(statements: List<ElectricityStatementDto>, residenceName: String): ByteArray {
        val out = ByteArrayOutputStream()
        
        // Page A4 Portrait
        val document = Document(PageSize.A4, 20f, 20f, 20f, 20f)
        val writer = PdfWriter.getInstance(document, out)
        document.open()

        val cb = writer.directContent

        // Table with 2 columns, spanning 100% width
        val table = PdfPTable(2)
        table.widthPercentage = 100f
        
        // We will display exactly 4 statements. If the list is smaller, we fill with empty receipt templates.
        for (i in 0 until 4) {
            val stmt = statements.getOrNull(i)
            val cell = PdfPCell()
            cell.fixedHeight = 390f
            cell.setPadding(15f)
            cell.border = Rectangle.NO_BORDER // We will draw cutting dashed lines instead

            if (stmt != null) {
                // Receipt Header
                val header = Paragraph("$residenceName\nREÇU D'ÉLECTRICITÉ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Font.BOLD))
                header.alignment = Element.ALIGN_CENTER
                cell.addElement(header)
                
                cell.addElement(Paragraph("\n"))
                
                // Receipt details
                cell.addElement(Paragraph("Date du relevé : ${stmt.statementDate}", FontFactory.getFont(FontFactory.HELVETICA, 10f)))
                cell.addElement(Paragraph("Logement : Unité ${stmt.logementId.take(8).uppercase()}", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)))
                
                cell.addElement(Paragraph("\nINDEXATION :", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Font.UNDERLINE)))
                cell.addElement(Paragraph("• Ancien Index : ${stmt.previousIndex} kWh", FontFactory.getFont(FontFactory.HELVETICA, 10f)))
                cell.addElement(Paragraph("• Nouveau Index : ${stmt.newIndex} kWh", FontFactory.getFont(FontFactory.HELVETICA, 10f)))
                
                val consumption = stmt.newIndex - stmt.previousIndex
                cell.addElement(Paragraph("• Consommation : $consumption kWh", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)))
                cell.addElement(Paragraph("• Tarif unitaire : ${stmt.kWhPriceApplied} XOF / kWh", FontFactory.getFont(FontFactory.HELVETICA, 10f)))
                
                cell.addElement(Paragraph("\n"))
                
                val footer = Paragraph("TOTAL DÛ : ${stmt.amountDue} XOF\nStatut : ${stmt.status.name}", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, Font.BOLD))
                footer.alignment = Element.ALIGN_RIGHT
                cell.addElement(footer)
            } else {
                // Empty template cell
                val emptyHeader = Paragraph("REÇU VIERGE (SANS RELEVÉ)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, Font.ITALIC))
                emptyHeader.alignment = Element.ALIGN_CENTER
                cell.addElement(emptyHeader)
            }

            table.addCell(cell)
        }

        document.add(table)

        // Draw dotted/dashed cutting lines
        val width = PageSize.A4.width
        val height = PageSize.A4.height

        cb.saveState()
        cb.setLineWidth(1f)
        cb.setLineDash(3f, 3f, 0f) // Set dotted/dashed pattern
        cb.setColorStroke(Color.GRAY)

        // Horizontal cutting line (centered at y = height / 2)
        cb.moveTo(0f, height / 2f)
        cb.lineTo(width, height / 2f)

        // Vertical cutting line (centered at x = width / 2)
        cb.moveTo(width / 2f, 0f)
        cb.lineTo(width / 2f, height)

        cb.stroke()
        cb.restoreState()

        document.close()
        return out.toByteArray()
    }
}

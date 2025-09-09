package com.nelumbo.park.utils;

import com.nelumbo.park.dto.response.VehicleOutDetailResponse;

public class HtmlGenerator {

    private HtmlGenerator() {}

    public static String generateHtmlContent(VehicleOutDetailResponse vehicle) {
        return String.format("""
            <body style="margin: 0px; padding: 0px; background: rgb(245, 245, 247); font-family: Arial, sans-serif;">
            <table align="center" width="100%%" cellpadding="0" cellspacing="0" style="padding: 20px; width: 100%%;">
                <tbody>
                <tr>
                    <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background: rgb(255, 255, 255); border-radius: 8px; padding: 30px; width: 100%%;">
                            <tbody>
                            <tr>
                                <td style="font-size:24px; font-weight:bold; color:#111; padding-bottom:10px;">Reporte de Vehículos</td>
                            </tr>
                            <tr>
                                <td style="font-size:16px; color:#333; line-height:1.5; padding-bottom:20px;">
                                    Hola <b>%s</b>,<br><br>
                                    En la última hora ha habido un total de <b>%d</b> vehículos en
                                    <b>%s</b>.<br>
                                    Puede consultar tanto el resumen como el detalle de los movimientos directamente en el archivo Excel.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:16px; color:#333; line-height:1.5; padding-bottom:20px;">
                                    Si necesitas más información, no dudes en contactar al equipo de soporte del sistema.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:16px; color:#333; line-height:1.5; padding-bottom:20px;">
                                    Este es un mensaje automático, no responda a este correo.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size:16px; color:#333; line-height:1.5; padding-bottom:20px;">
                                    Saludos,<br>Equipo de Soporte
                                </td>
                            </tr>
                            </tbody>
                        </table>
                    </td>
                </tr>
                </tbody>
            </table>
            </body>
            """, vehicle.getUsername(), vehicle.getTotalVehicles(), vehicle.getParking());
    }
}


package com.example.data

enum class SalesStage(
    val displayName: String,
    val shortName: String,
    val order: Int,
    val isClosed: Boolean = false
) {
    NUEVO("Nuevo", "Nuevo", 0),
    CONTACTADO("Contactado", "Contactado", 1),
    CITA_PRUEBA("Cita o prueba de manejo", "Cita / Prueba", 2),
    COTIZACION("Cotización enviada", "Cotización", 3),
    NEGOCIACION("Negociación", "Negociación", 4),
    VENDIDO("Vendido", "Vendido", 5, isClosed = true),
    PERDIDO("Perdido", "Perdido", 6, isClosed = true);

    companion object {
        val activeStages: List<SalesStage> = entries.filter { !it.isClosed }
    }
}

enum class LeadOrigin(val displayName: String) {
    AGENCIA("Agencia"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    MARKETPLACE("Marketplace"),
    RECOMENDACION("Recomendación"),
    LLAMADA("Llamada")
}

enum class VehicleCondition(val displayName: String) {
    NUEVO("Nuevo"),
    SEMINUEVO("Seminuevo")
}

enum class PaymentMethod(val displayName: String) {
    CONTADO("Contado"),
    CREDITO("Crédito")
}

enum class LossReason(val displayName: String) {
    PRECIO("Precio"),
    COMPRO_OTRO_LADO("Compró en otro lado"),
    CREDITO_RECHAZADO("Crédito rechazado"),
    SIN_INTERES("Sin interés")
}

enum class NoteType(val displayName: String) {
    LLAMADA("Llamada"),
    VISITA("Visita"),
    COTIZACION("Cotización"),
    PRUEBA_MANEJO("Prueba de manejo"),
    WHATSAPP("WhatsApp"),
    NOTA("Nota")
}

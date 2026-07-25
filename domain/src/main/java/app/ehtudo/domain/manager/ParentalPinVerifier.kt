package app.ehtudo.domain.manager

interface ParentalPinVerifier {
    suspend fun verifyParentalPin(pin: String): Boolean
}
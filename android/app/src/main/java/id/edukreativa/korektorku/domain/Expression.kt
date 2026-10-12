package id.edukreativa.korektorku.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Parser non-executable; accepts only number, approved variable, arithmetic, MIN, MAX, ROUND, CLAMP. */
class Expression(private val source: String, private val variables: Map<String, BigDecimal>) {
    private var i = 0
    private val mc = MathContext(18, RoundingMode.HALF_UP)
    private var operations = 0

    fun eval(): BigDecimal {
        require(source.length in 1..300) { "Rumus harus 1..300 karakter" }
        val v = sum()
        space()
        require(i == source.length) { "Karakter rumus tidak dikenal di posisi $i" }
        return check(v)
    }
    private fun check(v: BigDecimal): BigDecimal {
        require(v.precision() <= 32 && v.abs() <= BigDecimal("1000000000")) { "Hasil rumus terlalu besar" }
        require(++operations <= 150) { "Rumus terlalu kompleks" }
        return v
    }
    private fun space() { while (i < source.length && source[i].isWhitespace()) i++ }
    private fun eat(c: Char): Boolean { space(); if (i < source.length && source[i]==c) { i++; return true }; return false }
    private fun sum(): BigDecimal {
        var x = product()
        while (true) x = when { eat('+') -> check(x.add(product(),mc)); eat('-') -> check(x.subtract(product(),mc)); else -> return x }
    }
    private fun product(): BigDecimal {
        var x = unary()
        while (true) x = when { eat('*') -> check(x.multiply(unary(),mc)); eat('/') -> {
            val d = unary(); require(d.compareTo(BigDecimal.ZERO)!=0) { "Pembagian nol" }; check(x.divide(d,mc))
        }; else -> return x }
    }
    private fun unary(): BigDecimal = when { eat('-') -> unary().negate(); eat('+') -> unary(); else -> atom() }
    private fun atom(): BigDecimal {
        space()
        if (eat('(')) { val x=sum(); require(eat(')')) { "Kurung tutup tidak ditemukan" }; return x }
        require(i < source.length) { "Rumus tidak lengkap" }
        val start=i
        if (source[i].isDigit() || source[i]=='.') {
            var dots=0
            while(i<source.length && (source[i].isDigit() || source[i]=='.')) { if(source[i]=='.') dots++; i++ }
            require(dots<=1) { "Format angka tidak valid" }
            return check(BigDecimal(source.substring(start,i)))
        }
        if (source[i].isLetter() || source[i]=='_') {
            while(i<source.length && (source[i].isLetterOrDigit() || source[i]=='_')) i++
            val name=source.substring(start,i).uppercase()
            if(eat('(')) {
                val params=mutableListOf<BigDecimal>(); params.add(sum())
                while(eat(',')) { require(params.size<4) { "Parameter fungsi terlalu banyak" }; params.add(sum()) }
                require(eat(')')) { "Kurung fungsi tidak lengkap" }
                return check(when(name) {
                    "MIN" -> { require(params.size==2); params[0].min(params[1]) }
                    "MAX" -> { require(params.size==2); params[0].max(params[1]) }
                    "ROUND" -> { require(params.size==2); val dec=params[1].intValueExact(); require(dec in 0..4); params[0].setScale(dec,RoundingMode.HALF_UP) }
                    "CLAMP" -> { require(params.size==3 && params[1] <= params[2]); params[0].max(params[1]).min(params[2]) }
                    else -> throw IllegalArgumentException("Fungsi tidak dikenal: $name")
                })
            }
            return variables[name] ?: throw IllegalArgumentException("Variabel tidak dikenal: $name")
        }
        throw IllegalArgumentException("Karakter tidak diizinkan di posisi $i")
    }
}

package com.github.rwsbillyang.appproxy.utils

import android.util.Log
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.experimental.and
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

class CIDR() : Comparable<CIDR?> {
    var address: InetAddress? = null
    var prefix = 0

    constructor(address: InetAddress?, prefix: Int):this(){
        this.address = address
        this.prefix = prefix
    }

    constructor(ip: String?, prefix: Int):this() {
        try {
            address = InetAddress.getByName(ip)
            this.prefix = prefix
        } catch (ex: UnknownHostException) {
            Log.e("CIDR", ex.message?:"UnknownHostException")
        }
    }

    val start: InetAddress?
        get() = IPv4Util.long2inet(IPv4Util.inet2long(address) and IPv4Util.prefix2mask(prefix))
    val end: InetAddress?
        get() = IPv4Util.long2inet((IPv4Util.inet2long(address) and IPv4Util.prefix2mask(prefix)) + (1L shl 32 - prefix) - 1)

    override fun toString(): String {
        return address?.hostAddress + "/" + prefix + "=" + start?.hostAddress + "..." + end?.hostAddress
    }

    override operator fun compareTo(other: CIDR?): Int {
        val lcidr = IPv4Util.inet2long(address)
        val lother = IPv4Util.inet2long(other?.address)
        return lcidr.compareTo(lother)
    }
}

object IPv4Util {
    private const val TAG = "IPUtil"

    fun isValidIPv4Address(address: String): Boolean {
        if (address.isEmpty()) {
            return false
        }
        val parts = address.split(":").toTypedArray()
        if (parts.size > 1) {
           val port = try {
                parts[1].toInt()
            } catch (e: NumberFormatException) {
                Log.w(TAG, "NumberFormatException: $address")
                return false
            }
            if (port !in 1..65535) {
                return false
            }
        }
        val ipParts = parts[0].split("\\.").toTypedArray()
        if (ipParts.size != 4) {
            return false
        } else {
            for (i in ipParts.indices) {
                val ipPart = try {
                    ipParts[i].toInt()
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "NumberFormatException: $address")
                    return false
                }
                if (ipPart !in 0..255) {
                    return false
                }
            }
        }
        return true
    }

    @Throws(UnknownHostException::class)
    fun toCIDR(start: String?, end: String?): List<CIDR> {
        return toCIDR(InetAddress.getByName(start), InetAddress.getByName(end))
    }

    @Throws(UnknownHostException::class)
    fun toCIDR(start: InetAddress, end: InetAddress): List<CIDR> {
        val listResult: MutableList<CIDR> = ArrayList()
        Log.i(TAG, "toCIDR(" + start.hostAddress + "," + end.hostAddress + ")")
        var from = inet2long(start)
        val to = inet2long(end)
        while (to >= from) {
            var prefix: Byte = 32
            while (prefix > 0) {
                val mask = prefix2mask(prefix - 1)
                if (from and mask != from) break
                prefix--
            }
            val max =
                (32 - floor(ln((to - from + 1).toDouble()) / ln(2.0))).toInt().toByte()
            if (prefix < max) prefix = max
            listResult.add(CIDR(long2inet(from), prefix.toInt()))
            from += 2.0.pow((32 - prefix).toDouble()).toLong()
        }
        for (cidr in listResult) Log.i(TAG, cidr.toString())
        return listResult
    }



    fun prefix2mask(bits: Int): Long {
        return -0x100000000L shr bits and 0xFFFFFFFFL
    }

    fun inet2long(addr: InetAddress?): Long {
        var result: Long = 0
        if (addr != null) for (b in addr.address) result = result shl 8 or ((b and 0xFF.toByte()).toLong())
        return result
    }

    fun long2inet(address: Long): InetAddress? {
        var addr = address
        return try {
            val b = ByteArray(4)
            for (i in b.indices.reversed()) {
                b[i] = (addr and 0xFF).toByte()
                addr = addr shr 8
            }
            InetAddress.getByAddress(b)
        } catch (ignore: UnknownHostException) {
            null
        }
    }

    fun minus1(addr: InetAddress?): InetAddress? {
        return long2inet(inet2long(addr) - 1)
    }

    fun plus1(addr: InetAddress?): InetAddress? {
        return long2inet(inet2long(addr) + 1)
    }


}

package com.github.rwsbillyang.appproxy.utils

import android.content.Intent
import android.security.KeyChain
import android.util.Log
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import java.util.regex.Pattern


object CertificateUtil {
    private const val TAG = "CertificateManager"
    private val CA_COMMON_NAME = Pattern.compile("CN=([^,]+),?.*$")
    private val CA_ORGANIZATION = Pattern.compile("O=([^,]+),?.*$")
    fun findCAStore(caName: String?): Boolean {
        var found = false
        try {
            val ks = KeyStore.getInstance("AndroidCAStore") ?: return false
            ks.load(null, null)
            var rootCACert: X509Certificate? = null
            val aliases: Enumeration<*> = ks.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement() as String
                rootCACert = ks.getCertificate(alias) as X509Certificate
                if (rootCACert.issuerDN.name.contains(caName!!)) {
                    found = true
                    break
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } catch (e: KeyStoreException) {
            Log.e(TAG, e.message, e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, e.message, e)
        } catch (e: CertificateException) {
            Log.e(TAG, e.message, e)
        }
        return found
    }

    val rootCAStore: List<X509Certificate>?
        get() {
            val rootCAList: MutableList<X509Certificate> = ArrayList()
            try {
                val ks = KeyStore.getInstance("AndroidCAStore") ?: return null
                ks.load(null, null)
                val rootCACert: X509Certificate? = null
                val aliases: Enumeration<*> = ks.aliases()
                val found = false
                while (aliases.hasMoreElements()) {
                    val alias = aliases.nextElement() as String
                    val cert = ks.getCertificate(alias) as X509Certificate
                    println(alias + "/" + cert.issuerX500Principal.name)
                    rootCAList.add(cert)
                }
            } catch (e: IOException) {
                Log.e(TAG, e.message, e)
            } catch (e: KeyStoreException) {
                Log.e(TAG, e.message, e)
            } catch (e: NoSuchAlgorithmException) {
                Log.e(TAG, e.message, e)
            } catch (e: CertificateException) {
                Log.e(TAG, e.message, e)
            }
            return rootCAList
        }

    fun getRootCAMap(type: EnumSet<CertificateInstallType>): Map<String, Array<String>>? {
        val rootCAMap: MutableMap<String, Array<String>> = HashMap()
        try {
            val ks = KeyStore.getInstance("AndroidCAStore") ?: return null
            ks.load(null, null)
            val rootCACert: X509Certificate? = null
            val aliases: Enumeration<*> = ks.aliases()
            val certList: MutableList<X509Certificate> = ArrayList()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement() as String
                val cert = ks.getCertificate(alias) as X509Certificate
                if (type.contains(CertificateInstallType.SYSTEM) && alias.startsWith("system:")) {
                    certList.add(cert)
                }
                if (type.contains(CertificateInstallType.USER) && alias.startsWith("user:")) {
                    certList.add(cert)
                }
            }
            certList.sortWith(Comparator { o1, o2 ->
                val t1cn = getCommonName(o1.issuerX500Principal.name)
                val t2cn = getCommonName(o2.issuerX500Principal.name)
                t1cn.compareTo(t2cn, ignoreCase = true)
            })
            // ソート後
            val rootCANameList: MutableList<String> = ArrayList()
            val rootCAList: MutableList<String> = ArrayList()
            for (cert in certList) {
                val cn = getCommonName(cert.issuerX500Principal.name)
                if (cn.trim { it <= ' ' }.isEmpty()) continue
                //String o = CertificateUtil.getOrganization( cert.getIssuerX500Principal().getName());
                rootCANameList.add(cn)
                rootCAList.add(encode(cert.encoded))
            }
            rootCAMap["entry"] = rootCANameList.toTypedArray()
            rootCAMap["value"] = rootCAList.toTypedArray()
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } catch (e: KeyStoreException) {
            Log.e(TAG, e.message, e)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, e.message, e)
        } catch (e: CertificateException) {
            Log.e(TAG, e.message, e)
        }
        return rootCAMap
    }

    fun encode(b: ByteArray?): String {
        return String(b!!, StandardCharsets.ISO_8859_1)
    }

    fun decode(s: String): ByteArray {
        return s.toByteArray(StandardCharsets.ISO_8859_1)
    }

    fun trustRootCA(cert: X509Certificate): Intent? {
        Log.d(TAG, "root CA is not yet trusted")
        val intent = KeyChain.createInstallIntent()
        try {
            if (findCAStore(cert.issuerDN.name)) return null
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.encoded)
            intent.putExtra(KeyChain.EXTRA_NAME, getCommonName(cert.issuerDN.name))
        } catch (e: CertificateEncodingException) {
            Log.e(TAG, e.message, e)
        }
        return intent
    }

    // get the CA certificate by the path
    fun getCACertificate(buff: ByteArray?): X509Certificate? {
        var ca: X509Certificate? = null
        try {
            val cf = CertificateFactory.getInstance("X.509")
            ca = cf.generateCertificate(ByteArrayInputStream(buff)) as X509Certificate
        } catch (e: CertificateException) {
            Log.e(TAG, e.message, e)
        }
        return ca
    }

    // get the CA certificate by the path
    fun getCACertificate(caFile: File?): X509Certificate? {
        try {
            FileInputStream(caFile).use { inStream ->
                val cf =
                    CertificateFactory.getInstance("X.509")
                return cf.generateCertificate(inStream) as X509Certificate
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, e.message, e)
        } catch (e: IOException) {
            Log.e(TAG, e.message, e)
        } catch (e: CertificateException) {
            Log.e(TAG, e.message, e)
        }
        return null
    }

    fun getCommonName(dn: String?): String {
        var cn = ""
        val m = CA_COMMON_NAME.matcher(dn)
        if (m.find()) {
            cn = m.group(1)
        }
        return cn
    }

    fun getOrganization(dn: String?): String {
        var on = ""
        val m = CA_ORGANIZATION.matcher(dn)
        if (m.find()) {
            on = m.group(1)
        }
        return on
    }

    enum class CertificateInstallType {
        SYSTEM, USER
    }
}

package com.udemy.ocrlibrary

import java.util.Locale

object TextNormalizer {

    private val nikRegex = Regex("\\b[0-9OILDT ]{16,}\\b")

    private val correctionMap = mapOf(
        listOf("N1K", "NIK1", "N1KI", "IK", "IIK") to "NIK",
        listOf("NEMA", "NME", "NMA", "NAAM", "NNAMA", "NAMG", "LAMA", "AMA") to "Nama",
        listOf(
            "TEMPAT/GL", "TEMPAT / GL", "TEMPAT/GL", "TEMPAT/TG", "TMPT TGL", "TPT/TGL",
            "TPL/TGL", "TEMPAT/TGL", "TPLT/TGL", "TAMPAT/TGL", "TMPAT/TANGGAL", "TMPT",
            "TPLIGL", "TPL", "TMT TGL", "TEMPATIGL", "TEMPATHGL", "TEMPATTGL", "TEMPATGL",
            "TEMPATFGL", "TEMPATTGF", "EMPAT / TGL", "EMPAT/TGL", "TEMPATLGL", "EMPAT GL", "TEMPATTGL", "NPAT / TGI", "EMPATTGL", "MPAT / TGL LAHIR"
        ) to "Tempat/Tgl",
        listOf(
            "JK",
            "JNS KLAMIN",
            "JENIS KLAMIN",
            "JENIS KLMIN",
            "JNS KELMIN",
            "JNS KELAIN",
            "J K",
            "J.K.",
            "KELMIN",
            "KLAMIN",
            "GENDER",
            "JK L",
            "JENIS KELANIN",
            "JENIS KEIAMIN",
            "ENIS KELAMIN",
            "LENIS KELAMIN",
            "IS KELAMIN",
            "JENS KEA",
            "JENIS KELAN IN",
            "NIS KELAMIN"
        ) to "Jenis Kelamin",
        listOf(
            "LKILAKI", "LKILKI", "LAKILKI", "HAKILAKI", "LKILAKI", "LAKILAKI", "LAK1LAK1",
            "LAKHLAKE", "LAKLAK", "LAKI-LAKIE", "LAKELAKI", "LAKE-LAKI", "LAKE=LAKE",
            "LAKILAKE", "LAKHLAKI", "HAKILAKE", "LAKILAK", "LAKIAKI", "LAKIHLAKI", "LAKFLAKI"
        ) to "Laki-Laki",
        listOf(
            "PEREMPUAN",
            "PEREMPUA",
            "PEREMPUANN",
            "PEREMPUAAN",
            "PEREMPUA",
            "PREMPUAN",
            "PRMPUAN",
            "PRPUAN",
            "PRMMPUAN",
            "PRMPUAN"
        ) to "Perempuan",
        listOf("LENIS", "LENISS", "ENIS") to "Jenis",
        listOf(
            "ALAMT",
            "ALMT",
            "AMAT",
            "LAMAT",
            "ALMTT",
            "ALAAMT",
            "ALMAT",
            "ADDR",
            "ALNAMAT",
            "ALARNAT",
            "NAMAT"
        ) to "Alamat",
        listOf("RTW", "RTRW", "RIIRW", "RTRWE", "RTIRW", "RIRW", "ERT/RW", "RTAW") to "RT/RW",
        listOf(
            "KEVDESA",
            "KEDESA",
            "KELDES",
            "KELDS",
            "KEL/DESSA",
            "KEL/DESAA",
            "KEL/ DESSA",
            " KE / DESA",
            "KELDESA"
        ) to "Kel/Desa",
        listOf(
            "KECAATAN",
            "KECAMATAN",
            "KEC",
            "KECAMAATAN",
            "KECAMAATAN",
            "KECANATAN"
        ) to "Kecamatan",
        listOf(
            "AGM", "AGMA", "AGMMA", "AGMMAH", "AGMAMA", "AGGAMA", "AGAMAH", "GAMA"
        ) to "Agama",
        listOf("1SLAM", "ISLAME", "1SLAME", "JSLAM") to "Islam",
        listOf("KRISTEN", "KRIISTEN", "KRIISTE", "KRISTE", "KRISTN", "KRISTEEN") to "Kristen",
        listOf(
            "KATOLI",
            "KATOLK",
            "KATOLI",
            "KATOLIKK",
            "KATOLIKK",
            "KATHOLI",
            "KATHOLK",
            "KATHOLI",
            "KATHOLIKK",
            "KATHOLIKK",
            "KATHOEIK"
        ) to "Katholik",
        listOf(
            "HINDUU", " HIND ", " HINDUU ", " HINDUH ", " HINDUH "
        ) to " Hindu ",
        listOf(
            "BUDDHA", "BUDHHA", "BUDHHA"
        ) to "Budha",
        listOf(
            "KONGHUC", "KONGHUCUU", "KONGHUCU", "KONGHUCUU", "KGHUCU"
        ) to "Konghucu",
        listOf(
            "STATUS PRKAWINAN",
            "STS PERKAWINAN",
            "STATUS PERKWN",
            "STS PERKAWNN",
            "ST PERKAWINAN",
            "STAT PERKAWIN",
            "S T S PERKAWINAN",
            "STAT PRKWNN",
            "ATUS PERKAWINAN",
            "US PERKAWINAN",
            "TATUS PERKAWINAN",
            "LALUS PERKAWINAN",
            "ATUS PERKAWINA"
        ) to "Status Perkawinan",
        listOf(
            "PEKERJAAN",
            "PEKERJAAAN",
            "PEKERJAN",
            "PEKERJAANN",
            "PEKERJAA",
            "PKRJAN",
            "EKERJAAN",
            "ERJAAN"
        ) to "Pekerjaan",
        listOf(
            "PELAJARIMAHASISWA", "PELAJARIMAHASISW", "PELAJARIMAHASISWAA", "PELAJARMAHASISWA"
        ) to "Pelajar/Mahasiswa",
        listOf(
            "KEWARGANEGRAAN", "KWRGNN", "KWARGANEGARAAN", "KWGN", "KWARGANEGARAN", "KWRGNEGARA",
            "KEWARNEGARAAN", "KWARGAN", "WARGANEGARAAN", "EWARGANEGARAAN", "KEW ARGANEGARAAN", "VARGANEGARAAN", "CEWARGANEGARAAN"
        ) to "Kewarganegaraan",
        listOf("WNNI", "NNI", "WNE") to "Wni",
        listOf(
            "BERLAKU S/D", "BRLKU HINGGA", "BERLAKKUHIN", "BERLAKUHINGA", "BERLK HINGGA",
            "BRLAKU HNGGA", "BRLK S/D", "BRLKU S/D", "BRLAKUHNGG", "BERLAKUINGGA", "BERLAKU FNGGA",
            "BERLAKUFNGGA", "TAKU HINGGA", "BERAIU HINGGA", "ERLAKU HINGGA", "AKU HINGGA", "RLAKU HINGGA"
        ) to "Berlaku Hingga",
        listOf("JLN", "JALAN", "JL") to "Jalan"
    )

    fun normalizeText(text: String): String {
        var normalizedText = text
        normalizedText = nikRegex.replace(normalizedText) { matchResult ->
            var nik = matchResult.value
            nik = nik.replace(" ", "")
            nik = nik.replace("O", "0")
                .replace("T", "7")
                .replace("L", "1")
                .replace("I", "1")
                .replace("D", "0")

            nik
        }

        for ((wrongTexts, correctText) in correctionMap) {
            for (wrongText in wrongTexts) {
                val regex = Regex("\\b$wrongText\\b", RegexOption.IGNORE_CASE)
                normalizedText = normalizedText.replace(regex, correctText)
            }
        }
        normalizedText = normalizedText.replace("/", " / ")
            .replace(":", "")
            .replace("-", " - ")
            .replace(".", "")

        return normalizedText.uppercase(Locale.getDefault())
    }

}

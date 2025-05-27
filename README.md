## Codacy Grade
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/bcc44a15e80c4ff59d0c89de022bdc29)](https://app.codacy.com/gh/TashiRabten/MantraCountUI/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)

# 📿 MantraCount

A sophisticated cross-platform JavaFX application designed for Buddhist practitioners to count, analyze, and validate mantra practice logs from WhatsApp chat exports and text files.

---

## 🌟 Features / Recursos

### 📊 **Core Analysis**
- ✅ **Smart Mantra Counting** - Auto-detects mantras, "fiz", and practice keywords
- 🎯 **Synonym Support** - Recognizes variants like "Tare/Tara", "Vajrasattva/Vajrasatva"
- 🧮 **Comprehensive Statistics** - Total counts, daily analysis, and practice tracking
- 📅 **Missing Days Detection** - Identifies gaps in practice with context analysis

### 🔍 **Advanced Features**
- 🔎 **Powerful Search** - Find and edit entries with exact word matching
- ⚠️ **Mismatch Detection** - Flags inconsistencies while accepting valid synonyms
- 📝 **Inline Editing** - Edit problematic entries directly in the interface
- 🖼️ **Visual Recognition** - Displays appropriate deity/mantra images automatically
- 📊 **All Mantras View** - Comprehensive overview of all practices in date ranges
- 🌐 **Multi-format Support** - iPhone & Android WhatsApp exports

### 💾 **File Management**
- 📤 **Export & Save** - Overwrite original `.txt` or `.zip` files
- 🔄 **Auto-update** - Automatic updates for Windows and macOS
- 💼 **Batch Processing** - View all mantras across date ranges
- 🛡️ **Backup Protection** - Automatic backups before saving changes

### 🌍 **User Experience**
- ☑️ **Bilingual Interface** - Complete English/Portuguese support
- 🎨 **Modern UI** - Clean, intuitive design with tooltips
- ⚡ **Performance** - Optimized for large chat files
- 📱 **Cross-platform** - Windows, macOS, and Linux compatible

---

## 🖥️ Installation / Instalação

### ▶️ Windows
1. Download the latest `.exe` installer from:
   👉 [GitHub Releases](https://github.com/TashiRabten/MantraCountUI/releases)
2. Run the installer and follow the setup wizard
3. The app will automatically check for updates on future launches

### 🍏 macOS (Intel & Apple Silicon)
1. Download the `.pkg` file from:
   👉 [GitHub Releases](https://github.com/TashiRabten/MantraCountUI/releases)
2. Open the package and follow installation steps
3. Launch from Applications folder or Spotlight search

### 🐧 Linux
- Build from source using Maven (Java 17+ required)
- Pre-built packages coming soon

---

## 🚀 Quick Start / Início Rápido

1. **Export your WhatsApp chat** containing mantra practice logs
2. **Open MantraCount** and click "Abrir Arquivo" (Open File)
3. **Select your start date** and enter the mantra name (e.g., "Tare", "Vajrasattva")
4. **Click "Contar Mantras"** to analyze your practice
5. **Review results** and edit any flagged mismatches if needed
6. **Use "Dias Faltantes"** to find missing practice days
7. **Use "Todos os Mantras"** to view all mantras in a date range
8. **Save changes** back to your original file

### 📋 Test Data / Dados de Teste

Want to try MantraCount without your own data? Copy this sample WhatsApp chat into a `.txt` file:

```
[5/20/25, 8:30:15 AM] João Silva: Bom dia! Fiz 108 mantras de Tare ontem.
[5/21/25, 7:45:22 AM] Maria Santos: Recitei 54 mantras Vajrasattva esta manhã 🙏
[5/21/25, 6:30:10 PM] João Silva: Fiz 216 mantras de Tara hoje 
[5/22/25, 9:15:33 AM] Ana Costa: Completei 108 mantras de refugio
[5/23/25, 7:20:45 AM] João Silva: Fiz 324 mantras Vajrasatva (com uma pequena pausa)
[5/24/25, 8:45:18 AM] Maria Santos: 72 mantras de medicina Buddha feitos!
[5/25/25, 6:15:27 AM] Ana Costa: Manhã de prática: fiz 108 mantras Tare
[5/26/25, 7:55:12 AM] João Silva: Rito de Vajrasattva completo - fiz 108 mantras
```

**To test:**
1. Save the text above as `test_mantras.txt`
2. Open MantraCount and load this file
3. Set start date to `5/20/25` and search for `"Tare"` or `"Vajrasattva"`
4. Notice how synonyms like "Tara/Tare" and "Vajrasatva/Vajrasattva" are handled
5. Check "Todos os Mantras" to see all entries with appropriate deity images

---

## 🔄 Auto-Updates / Atualizações

- ✅ Automatic update checking on startup
- 🔄 Manual update check via the update button
- 📥 Automatic installer download and launch
- 🔄 Seamless version upgrades without data loss

---

## 🛠️ Technical Details / Detalhes Técnicos

### Supported Formats
- **WhatsApp iPhone exports**: `[DD/MM/YY, HH:MM:SS] Name: Message`
- **WhatsApp Android exports**: `DD/MM/YYYY HH:MM - Name: Message`
- **Plain text files**: Any format with recognizable dates
- **Compressed files**: `.zip` archives containing text files

### Visual Features
- **Deity Images**: Automatic display of appropriate images for Tara, Vajrasattva, Medicine Buddha, etc.
- **Mantra Type Recognition**: Visual badges showing mantra categories
- **Progress Indicators**: Real-time feedback during processing
- **Bilingual Tooltips**: Helpful hints in both English and Portuguese

### Date Format Detection
- Automatic BR (DD/MM/YY) vs US (MM/DD/YY) format detection
- Intelligent parsing with fallback mechanisms
- Support for both 2-digit and 4-digit years

### Synonym Recognition
- Built-in support for common mantra variants
- Extensible synonym system for custom terms
- Maintains consistency while accepting variations

---

## 🧪 Requirements / Requisitos

- **Runtime**: Java 17+ (bundled - no separate installation needed)
- **OS**: Windows 10+, macOS 12+ (Monterey), Linux (Ubuntu 20.04+)
- **Memory**: 256MB RAM minimum, 512MB recommended
- **Storage**: 50MB for application, additional space for log files

---

## 📚 Use Cases / Casos de Uso

- **Daily Practice Tracking** - Monitor your mantra recitation progress
- **Retreat Analysis** - Comprehensive statistics for intensive practice periods
- **Group Practice** - Analyze shared chat exports from practice groups
- **Long-term Studies** - Track practice consistency over months/years
- **Data Validation** - Ensure accuracy in practice logs and records

---

## 🤝 Contributing / Contribuição

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for:
- 🐛 Bug reports and feature requests
- 🔧 Code contributions and pull requests
- 🌍 Translations and localization
- 📖 Documentation improvements

---

## 📫 Support / Suporte

- **Issues & Bug Reports**: [GitHub Issues](https://github.com/TashiRabten/MantraCountUI/issues)
- **Feature Requests**: Use the "enhancement" label on GitHub Issues
- **Developer**: [Tashi Rabten](https://github.com/TashiRabten)
- **Documentation**: Check the [Wiki](https://github.com/TashiRabten/MantraCountUI/wiki) for detailed guides

---

## 📜 License / Licença

MIT License - This software is open-source and free to use, modify, and distribute.

**Built with 💜 for the Associação Buddha-Dharma (Buddha-Dharma Association) community**

---

*Last updated: May 2025 | Version 3.4+*
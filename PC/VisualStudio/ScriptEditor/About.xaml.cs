using System;
using System.Diagnostics;
using System.Globalization;
using System.Reflection;
using System.Windows;

namespace ScriptEditor
{
    /// <summary>
    /// Interaction logic for About.xaml
    /// </summary>
    public partial class About : Window
    {
        private static DateTime GetBuildDate(Assembly assembly)
        {
            const string BuildVersionMetadataPrefix = "+build";

            var attribute = assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>();
            if (attribute?.InformationalVersion != null)
            {
                var value = attribute.InformationalVersion;
                var index = value.IndexOf(BuildVersionMetadataPrefix);
                if (index > 0)
                {
                    value = value.Substring(index + BuildVersionMetadataPrefix.Length);
                    if (DateTime.TryParseExact(value, "yyyyMMddHHmmss", CultureInfo.InvariantCulture, DateTimeStyles.None, out var result))
                    {
                        return result;
                    }
                }
            }

            return default;
        }

        public About()
        {
            InitializeComponent();
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            Version version = Assembly.GetEntryAssembly().GetName().Version;
            DateTime bld = GetBuildDate(Assembly.GetEntryAssembly());
            //CurrentVersion.Text = "Версия: " + version.ToString();
            CurrentVersion.Text = "Версия: " + version.ToString() + " (Сборка от " + bld.ToString() + ")";
        }

        private void btnDialogOk_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private void Link_Click(object sender, RoutedEventArgs e)
        {
            Process myProcess = new Process();

            try
            {
                // true is the default, but it is important not to set it to false
                myProcess.StartInfo.UseShellExecute = true;
                myProcess.StartInfo.FileName = Link.NavigateUri.ToString();
                myProcess.Start();
            }
            catch
            {
            }
        }
    }
}

using GMap.NET;
using GMap.NET.MapProviders;
using GMap.NET.WindowsPresentation;
using Microsoft.Win32;
using NavControlLibrary.Map;
using NavControlLibrary.Models;
using Newtonsoft.Json;
using ScriptEditor.Views;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using ToastNotifications;
using ToastNotifications.Core;
using ToastNotifications.Lifetime;
using ToastNotifications.Messages;
using ToastNotifications.Position;
using static NavControlLibrary.Models.RouteModel;

namespace ScriptEditor
{
    /// <summary>
    /// Interaction logic for MainWindow.xaml
    /// </summary>
    public partial class MainWindow : Window
    {
        RouteModel mModel = new RouteModel();

        DispatcherTimer timer = new DispatcherTimer();
        DispatcherTimer timer2 = new DispatcherTimer();

        public MainWindow()
        {
            InitializeComponent();

            MainMap.ShowCenter = false;
            timer.Interval = TimeSpan.FromMilliseconds(100);
            timer.Tick += timer_Tick;
            timer2.Interval = TimeSpan.FromMilliseconds(300);
            timer2.Tick += timer2_Tick;

            DataContext = mModel;
            mModel.onModelChanged += MModel_onModelChanged;
            mModel.onAddGPSTrigger += MModel_onAddGPSTrigger;
            mModel.onRemoveGPSTrigger += MModel_onRemoveGPSTrigger;
        }

        private void MModel_onModelChanged(NavControlLibrary.IModelChanged sender)
        {
            Title += "*";
        }


        void timer_Tick(object sender, EventArgs e)
        {
            timer.Stop();
            RefreshMarkers();
            Wait.Visibility = Visibility.Hidden;
        }
        void timer2_Tick(object sender, EventArgs e)
        {
            timer2.Stop();
            RefreshMarkers(true);
            Wait.Visibility = Visibility.Hidden;
        }

        private void RefreshMainMap(object sender, System.ComponentModel.ListChangedEventArgs e)
        {
            BindingList<ScriptModel> lst = sender as BindingList<ScriptModel>;
            if (e.ListChangedType == System.ComponentModel.ListChangedType.ItemAdded)
            {
                timer.Start();
            }
            else if (e.ListChangedType == System.ComponentModel.ListChangedType.ItemDeleted)
            {
                timer.Start();
            }
        }

        private void RefreshMainMap3(object sender, System.ComponentModel.ListChangedEventArgs e)
        {
            if (e.ListChangedType == System.ComponentModel.ListChangedType.ItemAdded)
            {
                GPSMarker marker = new GPSMarker(mModel.Scripts[e.NewIndex].GPSTrigger, Color.FromArgb(50, 255, 255, 0), mModel.Scripts[e.NewIndex].Name);
                mMarkers3.Add(marker);
                marker.AddToMap(MainMap);
            }
            else if (e.ListChangedType == System.ComponentModel.ListChangedType.ItemDeleted)
            {
                GPSMarker marker = null;
                foreach (var x in mMarkers3)
                {
                    bool f = true;
                    foreach (var y in mModel.Scripts)
                    {
                        if (x.mModel == y.GPSTrigger)
                        {
                            f = false;
                            break;
                        }
                    }
                    if (f)
                    {
                        marker = x;
                        break;
                    }
                }
                if (marker != null)
                {
                    marker.RemoveFromMap();
                    mMarkers3.Remove(marker);
                }
            }
        }

        List<GPSMarker> mMarkers1 = new List<GPSMarker>();
        List<GPSMarker> mMarkers2 = new List<GPSMarker>();
        List<GPSMarker> mMarkers3 = new List<GPSMarker>();

        private void MModel_onRemoveGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script)
        {
            if (script.mDir == null)
            {
                GPSMarker marker = mMarkers3.FirstOrDefault(x => x.mModel == gpstrigger);
                if (marker != null)
                {
                    marker.RemoveFromMap();
                    mMarkers3.Remove(marker);
                }
            }
            else if (script.mDir == DIR_TYPES.FORWARD)
            {
                GPSMarker marker = mMarkers1.FirstOrDefault(x => x.mModel == gpstrigger);
                if (marker != null)
                {
                    marker.RemoveFromMap();
                    mMarkers1.Remove(marker);
                }
            }
            else if (script.mDir == DIR_TYPES.BACKWARD)
            {
                GPSMarker marker = mMarkers2.FirstOrDefault(x => x.mModel == gpstrigger);
                if (marker != null)
                {
                    marker.RemoveFromMap();
                    mMarkers2.Remove(marker);
                }
            }
        }

        private void MModel_onAddGPSTrigger(GPSTriggerModel gpstrigger, ScriptModel script)
        {
            if (script.mDir == null)
            {
                GPSMarker marker = new GPSMarker(gpstrigger, Color.FromArgb(50, 255, 255, 0), script.Name);
                mMarkers3.Add(marker);
                marker.AddToMap(MainMap);
                marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
            }
            else if (script.mDir == DIR_TYPES.FORWARD)
            {
                GPSMarker marker = new GPSMarker(gpstrigger, Color.FromArgb(50, 0, 0, 255), script.Name + "(вперёд)");
                mMarkers1.Add(marker);
                marker.AddToMap(MainMap);
                marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
            }
            else if (script.mDir == DIR_TYPES.BACKWARD)
            {
                GPSMarker marker = new GPSMarker(gpstrigger, Color.FromArgb(50, 0, 255, 0), script.Name + "(назад)");
                mMarkers2.Add(marker);
                marker.AddToMap(MainMap);
                marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
            }
        }

        void RefreshMarkers(bool zoom = false)
        {
            List<double> latList = new List<double>();
            List<double> lonList = new List<double>();
            List<GMapMarker> lst = new List<GMapMarker>();
            MainMap.Markers.Clear();
            mMarkers1.Clear();
            foreach (var itm in mModel.ForwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 0, 0, 255), itm.Name + "(вперёд)");
                    mMarkers1.Add(marker);
                    marker.AddToMap(MainMap);
                    marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }
            mMarkers2.Clear();
            foreach (var itm in mModel.BackwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 0, 255, 0), itm.Name + "(назад)");
                    mMarkers2.Add(marker);
                    marker.AddToMap(MainMap);
                    marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }
            mMarkers3.Clear();
            foreach (var itm in mModel.Scripts)
            {
                if (itm.IsGPSTrigger)
                {
                    GPSMarker marker = new GPSMarker(itm.GPSTrigger, Color.FromArgb(50, 255, 255, 0), itm.Name);
                    mMarkers3.Add(marker);
                    marker.AddToMap(MainMap);
                    marker.onSelectGPSTrigger += Marker_onSelectGPSTrigger;
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }

            //MainMap.Markers.Add
            if (zoom && (lonList.Count != 0))
            {
                double lnMax = lonList.Max();
                double lnMin = lonList.Min();
                double ltMax = latList.Max();
                double ltMin = latList.Min();
                MainMap.SetZoomToFitRect(new RectLatLng(ltMax + 0.0001, lnMin - 0.0001, lnMax - lnMin + 0.0002, ltMax - ltMin + 0.0002));
            }
        }

        private void Marker_onSelectGPSTrigger(GPSTriggerModel gpstrigger)
        {
            foreach (var itm in mModel.ForwardStops)
            {
                if (itm.GPSTrigger == gpstrigger)
                {
                    mModel.ForwardStopEdit = itm;
                    Dispatcher.BeginInvoke((Action)(() => MainTabs.SelectedIndex = 1));
                    Dispatcher.BeginInvoke((Action)(() => ForwardTabs.SelectedIndex = 1));
                    return;
                }
            }
            foreach (var itm in mModel.BackwardStops)
            {
                if (itm.GPSTrigger == gpstrigger)
                {
                    mModel.BackwardStopEdit = itm;
                    Dispatcher.BeginInvoke((Action)(() => MainTabs.SelectedIndex = 2));
                    Dispatcher.BeginInvoke((Action)(() => BackwardTabs.SelectedIndex = 1));
                    return;
                }
            }
            foreach (var itm in mModel.Scripts)
            {
                if (itm.GPSTrigger == gpstrigger)
                {
                    mModel.ScriptEdit = itm;
                    Dispatcher.BeginInvoke((Action)(() => MainTabs.SelectedIndex = 3));
                    Dispatcher.BeginInvoke((Action)(() => ScriptTabs.SelectedIndex = 1));
                    return;
                }
            }
        }

        private void Refresh_MainMapZoom(object sender, RoutedEventArgs e)
        {
            List<double> latList = new List<double>();
            List<double> lonList = new List<double>();
            foreach (var itm in mModel.ForwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }
            foreach (var itm in mModel.BackwardStops)
            {
                if (itm.IsGPSTrigger)
                {
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }
            foreach (var itm in mModel.Scripts)
            {
                if (itm.IsGPSTrigger)
                {
                    latList.Add(itm.GPSTrigger.Latitude);
                    lonList.Add(itm.GPSTrigger.Longitude);
                }
            }

            if (lonList.Count != 0)
            {
                double lnMax = lonList.Max();
                double lnMin = lonList.Min();
                double ltMax = latList.Max();
                double ltMin = latList.Min();
                MainMap.SetZoomToFitRect(new RectLatLng(ltMax + 0.0001, lnMin - 0.0001, lnMax - lnMin + 0.0002, ltMax - ltMin + 0.0002));
            }
        }

        private void Import_File(object sender, RoutedEventArgs e)
        {
            OpenFileDialog openFileDialog1 = new OpenFileDialog
            {
                Title = "Импортировать файл маршрута",
                DefaultExt = "jsonc",
                Filter = "jsonc files (*.jsonc)|*.jsonc"
            };
            if (openFileDialog1.ShowDialog() == true)
            {
                Import(openFileDialog1.FileName);
            }
        }

        private void Import(string filename)
        {
            try
            {
                var str = mModel.Import(filename);
                if (str == null)
                {
                    this.Title = "Редактор маршрута: " + filename;
                    mModel.LastFile = filename;
                }
                Wait.Visibility = Visibility.Visible;
                timer2.Start();
            }
            catch
            {

            }
        }

        private void Export_File(object sender, RoutedEventArgs e)
        {
            SaveFileDialog saveFileDialog1 = new SaveFileDialog
            {
                Title = "Экпортировать файл маршрута",
                DefaultExt = "jsonc",
                Filter = "jsonc files (*.jsonc)|*.jsonc"
            };
            if (saveFileDialog1.ShowDialog() == true)
            {
                var res = mModel.Export(saveFileDialog1.FileName);
                this.Title = "Редактор маршрута: " + saveFileDialog1.FileName;
                mModel.LastFile = saveFileDialog1.FileName;
            }
        }

        Update mUpdate = null;
        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            mUpdate = new Update("https://romasty.duckdns.org");
            mUpdate.PropertyChanged += MUpdate_PropertyChanged;

            Top = Properties.Settings.Default.Top;
            Left = Properties.Settings.Default.Left;
            Width = Properties.Settings.Default.Width;
            Height = Properties.Settings.Default.Height;
            WindowState = (WindowState)WindowState.Parse(typeof(WindowState), Properties.Settings.Default.WindowState);

            ControlGrid.Width = new GridLength(Properties.Settings.Default.ControlGridWidth);
            Script1.IsMap = Properties.Settings.Default.Map1;
            Script2.IsMap = Properties.Settings.Default.Map2;
            Script3.IsMap = Properties.Settings.Default.Map3;

            try
            {
                var lst = JsonConvert.DeserializeObject<List<RouteModel.ResentFileData>>(Properties.Settings.Default.ResentFiles);
                if (lst != null)
                {
                    foreach (var itm in lst)
                    {
                        if (File.Exists(itm.fileName)) mModel.RecentFiles.Add(itm);
                    }
                }
            }
            catch
            {

            }
            mModel.MapProvider = GMapProviders.TryGetProvider(Properties.Settings.Default.MapProvider);

            if (File.Exists(Properties.Settings.Default.LastFile))
            {
                if (Properties.Settings.Default.LastFile.EndsWith(".jsonc"))
                {
                    Import(Properties.Settings.Default.LastFile);
                }
            }
        }

        Notifier mNotifier = new Notifier(cfg =>
        {
            cfg.PositionProvider = new WindowPositionProvider(
                parentWindow: Application.Current.MainWindow,
                corner: Corner.BottomRight,
                offsetX: 10,
                offsetY: 10);

            cfg.LifetimeSupervisor = new TimeAndCountBasedLifetimeSupervisor(
                notificationLifetime: TimeSpan.FromSeconds(15),
                maximumNotificationCount: MaximumNotificationCount.FromCount(5));

            cfg.Dispatcher = Application.Current.Dispatcher;
        });

        private void MUpdate_PropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            if (e.PropertyName == "IsNew")
            {
                mModel.IsNew = mUpdate.IsNew;
                if (mUpdate.IsNew)
                {
                    if (String.Compare(Properties.Settings.Default.NewVersion, mUpdate.Version) < 0)
                    {
                        var options = new MessageOptions
                        {
                            //FontSize = 30, // set notification font size
                            //ShowCloseButton = false, // set the option to show or hide notification close button
                            //Tag = mUpdate.SetupUri,
                            FreezeOnMouseEnter = true, // set the option to prevent notification dissapear automatically if user move cursor on it
                            NotificationClickAction = n => // set the callback for notification click event
                            {
                                n.Close(); // call Close method to remove notification
                                Process myProcess = new Process();
                                try
                                {
                                    // true is the default, but it is important not to set it to false
                                    myProcess.StartInfo.UseShellExecute = true;
                                    myProcess.StartInfo.FileName = mUpdate.SetupUri;
                                    myProcess.Start();
                                }
                                catch
                                {
                                    mNotifier.ShowError("Ошибка адреса");
                                }
                            },
                            //CloseClickAction = n => {
                            //    var opts = obj.DisplayPart.GetOptions();
                            //    _vm.ShowInformation($"Notification close clicked, Tag: {opts.Tag}");
                            //},
                        };
                        mNotifier.ShowInformation("Новая версия приложения: " + mUpdate.Version, options);
                    }
                    Properties.Settings.Default.NewVersion = mUpdate.Version;
                }
            }
        }

        private void Window_Closed(object sender, EventArgs e)
        {
            Properties.Settings.Default.Top = Top;
            Properties.Settings.Default.Left = Left;
            Properties.Settings.Default.Width = Width;
            Properties.Settings.Default.Height = Height;
            Properties.Settings.Default.WindowState = WindowState.ToString();

            Properties.Settings.Default.ControlGridWidth = ControlGrid.Width.Value;
            Properties.Settings.Default.Map1 = Script1.IsMap;
            Properties.Settings.Default.Map2 = Script2.IsMap;
            Properties.Settings.Default.Map3 = Script3.IsMap;

            string output = JsonConvert.SerializeObject(mModel.RecentFiles);
            Properties.Settings.Default.ResentFiles = JsonConvert.SerializeObject(mModel.RecentFiles);
            Properties.Settings.Default.MapProvider = mModel.MapProvider.ToString();

            Properties.Settings.Default.LastFile = mModel.LastFile;

            Properties.Settings.Default.Save();
        }

        private void MainMap_Loaded(object sender, RoutedEventArgs e)
        {
            var x = MainMap.Zoom;
            MainMap.Zoom -= 0.0001;
            MainMap.Zoom = x;
        }

        private void TabItem11_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (ForwardTabs.SelectedIndex == 1)
                {
                    Dispatcher.BeginInvoke((Action)(() => ForwardTabs.SelectedIndex = 0));
                }
            }
            else
            {
                Dispatcher.BeginInvoke((Action)(() => ForwardTabs.SelectedIndex = 1));
            }
        }

        private void TabItem12_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (ForwardTabs.SelectedIndex == 2)
                {
                    Dispatcher.BeginInvoke((Action)(() => ForwardTabs.SelectedIndex = 0));
                }
            }
        }

        private void TabItem21_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (BackwardTabs.SelectedIndex == 1)
                {
                    Dispatcher.BeginInvoke((Action)(() => BackwardTabs.SelectedIndex = 0));
                }
            }
            else
            {
                Dispatcher.BeginInvoke((Action)(() => BackwardTabs.SelectedIndex = 1));
            }
        }

        private void TabItem22_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (BackwardTabs.SelectedIndex == 2)
                {
                    Dispatcher.BeginInvoke((Action)(() => BackwardTabs.SelectedIndex = 0));
                }
            }
        }

        private void TabItem31_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (ScriptTabs.SelectedIndex == 1)
                {
                    Dispatcher.BeginInvoke((Action)(() => ScriptTabs.SelectedIndex = 0));
                }
            }
            //else
            //{
            //    if ((ScriptTabs.Items[1] as TabItem).IsVisible)
            //    {
            //        Dispatcher.BeginInvoke((Action)(() => ScriptTabs.SelectedIndex = 1));
            //    }
            //}
        }

        private void TabItem32_IsVisibleChanged(object sender, DependencyPropertyChangedEventArgs e)
        {
            bool x = (bool)e.NewValue;
            if (!x)
            {
                if (ScriptTabs.SelectedIndex == 2)
                {
                    Dispatcher.BeginInvoke((Action)(() => ScriptTabs.SelectedIndex = 0));
                }
            }
        }

        private void Window_Closing(object sender, CancelEventArgs e)
        {
            Focus();

            if (mModel.IsChanged)
            {
                var win = new ClosingWindow();
                if (win.ShowDialog() == true)
                {
                    e.Cancel = true;
                }
                else if (win.Save)
                {
                    if (mModel.IsDefImport) mModel.Export(mModel.LastFile);
                    else
                    {
                        SaveFileDialog saveFileDialog1 = new SaveFileDialog
                        {
                            Title = "Экпортировать файл маршрута",
                            DefaultExt = "jsonc",
                            Filter = "jsonc files (*.jsonc)|*.jsonc"
                        };
                        if (saveFileDialog1.ShowDialog() == true)
                        {
                            mModel.Export(saveFileDialog1.FileName);
                            mModel.LastFile = saveFileDialog1.FileName;
                        }
                    }
                }
            }
        }

        private void GPSTriggerPage_onMapLockChanged(GPSTriggerPage sender, bool mapunlock)
        {
            GPS1.SetMapUnLock(mapunlock);
            GPS2.SetMapUnLock(mapunlock);
            GPS3.SetMapUnLock(mapunlock);
        }

        private void OpenFromFileList(object sender, RoutedEventArgs e)
        {
            string filename = (sender as Button).Tag as string;
            if (File.Exists(filename))
            {
                if (filename.EndsWith(".jsonc")) Import(filename);
            }
        }

        private void RemoveFileFromList(object sender, RoutedEventArgs e)
        {
            ResentFileData fl = (sender as Button).Tag as ResentFileData;
            mModel.RecentFiles.Remove(fl);
        }

        private void NewModel(object sender, RoutedEventArgs e)
        {
            Title = "";
            mModel.Reset();
            mModel.LastFile = "";
            Wait.Visibility = Visibility.Visible;
            timer2.Start();
        }

        private void Grid_MouseLeftButtonDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
        {
            if (e.ClickCount == 2)
            {
                string filename = (sender as Grid).Tag as string;
                if (File.Exists(filename))
                {
                    if (filename.EndsWith(".jsonc")) Import(filename);
                }
            }
        }

        private void Export_Default(object sender, RoutedEventArgs e)
        {
            var res = mModel.Export(mModel.LastFile);
            this.Title = "Редактор маршрута: " + mModel.LastFile;
        }

        private void ScriptTabs_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (mModel.ScriptEdit.IsGPSTrigger) Dispatcher.BeginInvoke((Action)(() => ScriptTabs.SelectedIndex = 1));
        }

        private void About_Click(object sender, RoutedEventArgs e)
        {
            var win = new About();
            if (mUpdate.IsNew)
            {
                win.Link.NavigateUri = new Uri(mUpdate.SetupUri);
                win.hyperlinkText.Text = "Новая версия: " + mUpdate.Version;
            }
            else win.NewVersion.Visibility = Visibility.Collapsed;

            win.ShowDialog();
        }
    }
}

using GMap.NET;
using GMap.NET.MapProviders;
using GMap.NET.WindowsPresentation;
using NavControlLibrary.Map;
using NavControlLibrary.Models;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;

namespace ScriptEditor.Views
{
    /// <summary>
    /// Interaction logic for ScriptListBox.xaml
    /// </summary>
    public partial class ScriptListBox : UserControl
    {
        #region Поля
        protected static readonly DependencyProperty ScriptBindingListProperty;
        protected static readonly DependencyProperty SelectedScriptProperty;
        protected static readonly DependencyProperty DirProperty;
        protected static readonly DependencyProperty MapProviderProperty;
        protected static readonly DependencyProperty IsMapProperty;

        static ScriptListBox()
        {
            ScriptBindingListProperty = DependencyProperty.Register("ScriptBindingList", typeof(BindingList<ScriptModel>), typeof(ScriptListBox),
                new FrameworkPropertyMetadata(null, changeScriptList));
            SelectedScriptProperty = DependencyProperty.Register("SelectedScript", typeof(ScriptModel), typeof(ScriptListBox),
                new FrameworkPropertyMetadata(null, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault, changeScript));
            DirProperty = DependencyProperty.Register("Dir", typeof(DIR_TYPES?), typeof(ScriptListBox),
                new FrameworkPropertyMetadata(null));
            MapProviderProperty = DependencyProperty.Register("MapProvider", typeof(GMapProvider), typeof(ScriptListBox),
                new FrameworkPropertyMetadata(GMapProviders.YandexMap));
            IsMapProperty = DependencyProperty.Register("IsMap", typeof(bool), typeof(ScriptListBox),
                new FrameworkPropertyMetadata(false, FrameworkPropertyMetadataOptions.BindsTwoWayByDefault));
        }

        private static void changeScriptList(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            ScriptListBox sb = (ScriptListBox)d;
            if (e.NewValue != null) sb.RefreshList((BindingList<ScriptModel>)e.NewValue);
        }

        private static void changeScript(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            ScriptListBox sb = (ScriptListBox)d;
            if (e.OldValue != null)
            {
                ScriptModel s = (ScriptModel)e.OldValue;
                s.onChangeGPSTrigger -= sb.ScriptChangeGPSTrigger;
            }
            if (e.NewValue != null)
            {
                ScriptModel s = (ScriptModel)e.NewValue;
                sb.RefreshMarker(s);
                s.onChangeGPSTrigger += sb.ScriptChangeGPSTrigger;
            }
        }

        private void ScriptChangeGPSTrigger(ScriptModel script)
        {
            RefreshList(ScriptBindingList);
        }

        GMapMarker mStop = new GMapMarker(new PointLatLng());
        MapDot mStopDot = new MapDot();
        private void RefreshMarker(ScriptModel s)
        {
            if (s == null)
            {
                RouteMap.Markers.Clear();
                return;
            }
            if (s.GPSTrigger != null)
            {
                mStop.Position = new(s.GPSTrigger.Latitude, s.GPSTrigger.Longitude);
                if (RouteMap.Markers.Count == 1) RouteMap.Markers.Add(mStop);
            }
            else
            {
                if (RouteMap.Markers.Count == 2) RouteMap.Markers.RemoveAt(1);
            }
        }
        #endregion Поля

        [Description("Список скриптов"), Category("Data")]
        public BindingList<ScriptModel> ScriptBindingList
        {
            get { return (BindingList<ScriptModel>)GetValue(ScriptBindingListProperty); }
            set { SetValue(ScriptBindingListProperty, value); }
        }

        [Description("Выбранный скрипт"), Category("Data")]
        public ScriptModel SelectedScript
        {
            get { return (ScriptModel)GetValue(SelectedScriptProperty); }
            set { SetValue(SelectedScriptProperty, value); }
        }

        [Description("Направление движения"), Category("Data")]
        public DIR_TYPES? Dir
        {
            get { return (DIR_TYPES?)GetValue(DirProperty); }
            set { SetValue(DirProperty, value); }
        }

        [Description("Источник карт"), Category("Data")]
        public GMapProvider MapProvider
        {
            get { return (GMapProvider)GetValue(MapProviderProperty); }
            set { SetValue(MapProviderProperty, value); }
        }

        [Description("Показать карту"), Category("Data")]
        public bool IsMap
        {
            get { return (bool)GetValue(IsMapProperty); }
            set { SetValue(IsMapProperty, value); }
        }


        public ScriptListBox()
        {
            InitializeComponent();
            //RouteMap.MapProvider = GMapProviders.YandexMap;
            RouteMap.ShowCenter = false;
            RouteMap.CanDragMap = false;
            RouteMap.MouseWheelZoomEnabled = false;

            mStop.Shape = mStopDot;
            mStop.Offset = new Point(-3, -3);
            mStopDot.Width = 5;
            mStopDot.Height = 5;

            DispatcherTimer timer = new DispatcherTimer();
            timer.Interval = TimeSpan.FromSeconds(1);
            timer.Tick += timer_Tick;
            timer.Start();
        }
        void timer_Tick(object sender, EventArgs e)
        {
            //if(!mZoom && (RouteMap.Visibility == Visibility.Visible))Refresh();
            if (RouteMap.Visibility == Visibility.Visible) Refresh();
        }

        private void Delete(object sender, RoutedEventArgs e)
        {
            int index = ScriptBindingList.IndexOf(SelectedScript);
            SelectedScript.IsGPSTrigger = false;
            ScriptBindingList.Remove(SelectedScript);
            if (index == ScriptBindingList.Count) index = ScriptBindingList.Count - 1;
            if (index >= 0)
            {
                RefreshList(ScriptBindingList);
                SelectedScript = ScriptBindingList[index];
            }
            else
            {
                SelectedScript = null;
            }
        }

        public void Refresh()
        {
            RefreshList(ScriptBindingList);
        }

        bool mZoom = false;
        private void RefreshList(BindingList<ScriptModel> lst)
        {
            RouteMap.Markers.Clear();
            if ((lst == null) || (lst.Count == 0)) return;
            List<PointLatLng> points = new List<PointLatLng>();

            int i = 1;
            ScriptModel prev = null;
            foreach (var itm in lst)
            {
                itm.ID = i++;
                itm.ListSize = lst.Count;
                itm.PrevStop(prev);
                if (prev != null) prev.NextStop(itm);
                prev = itm;
                if (itm.GPSTrigger != null)
                {
                    points.Add(new PointLatLng(itm.GPSTrigger.Latitude, itm.GPSTrigger.Longitude));
                }
            }
            if (prev != null) prev.NextStop(null);

            if (points.Count == 0) return;

            GMapRoute lines = new GMapRoute(points);
            RouteMap.Markers.Add(lines);

            if (points.Count != 0)
            {
                double lnMax = points.Max(obj => obj.Lng);
                double lnMin = points.Min(obj => obj.Lng);
                double ltMax = points.Max(obj => obj.Lat);
                double ltMin = points.Min(obj => obj.Lat);
                mZoom = RouteMap.SetZoomToFitRect(new RectLatLng(ltMax, lnMin, lnMax - lnMin, ltMax - ltMin));
            }

            RefreshMarker(SelectedScript);
        }


        private void Down(object sender, RoutedEventArgs e)
        {
            int index = ScriptBindingList.IndexOf(SelectedScript);
            ScriptModel model = SelectedScript;
            ScriptBindingList.Remove(SelectedScript);
            ScriptBindingList.Insert(index + 1, model);
            SelectedScript = model;
            RefreshList(ScriptBindingList);
        }

        private void Up(object sender, RoutedEventArgs e)
        {
            int index = ScriptBindingList.IndexOf(SelectedScript);
            ScriptModel model = SelectedScript;
            ScriptBindingList.Remove(SelectedScript);
            ScriptBindingList.Insert(index - 1, model);
            SelectedScript = model;
            RefreshList(ScriptBindingList);
        }

        private void New(object sender, RoutedEventArgs e)
        {
            if (ScriptBindingList.Count == 0)
            {
                SelectedScript = new ScriptModel();
                ScriptBindingList.Add(SelectedScript);
            }
            else
            {
                if (SelectedScript.IsGPSTrigger)
                {
                    SelectedScript.GPSTrigger.InitLast();
                }
                int index = ScriptBindingList.IndexOf(SelectedScript);
                SelectedScript = new ScriptModel();
                ScriptBindingList.Insert(index + 1, SelectedScript);
            }
            SelectedScript.Dir = Dir;
            SelectedScript.Priority = 10;
            if (Dir != null) SelectedScript.IsGPSTrigger = true;
            else SelectedScript.Name = "Скрипт";
            RefreshList(ScriptBindingList);
        }


        //private delegate void pTileLoadComplete(long elapsedMillisecond);
        //private void RouteMap_OnTileLoadComplete(long elapsedMilliseconds)
        //{
        //    if (!System.Windows.Application.Current.Dispatcher.CheckAccess())
        //    {
        //        pTileLoadComplete d = new pTileLoadComplete(RouteMap_OnTileLoadComplete);
        //        System.Windows.Application.Current.Dispatcher.Invoke(d, new object[] { elapsedMilliseconds });
        //    }
        //    else
        //    {
        //    }
        //}
    }
}

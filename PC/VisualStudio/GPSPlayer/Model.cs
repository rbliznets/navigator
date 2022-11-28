using MySql.Data.MySqlClient;
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Windows.Threading;

namespace GPSPlayer
{
    public class Model : NavControlLibrary.NotifyModel
    {
        class GPSData
        {
            public ulong sec;
            public ulong prev;
            public string data;
            public DateTime time;

            public GPSData(string logline, GPSData previous)
            {
                var cultureInfo = new CultureInfo("ru-RU");
                var index = logline.IndexOf('{');
                time = DateTime.Parse(logline.Substring(0, index), cultureInfo);
                data = logline.Substring(index).Trim();
                init(previous);
            }


            public void init(GPSData previous)
            {
                if (previous == null) sec = 0;
                else
                {
                    TimeSpan span = time.Subtract(previous.time);
                    prev = (ulong)span.TotalSeconds;
                    sec = previous.sec + prev;
                }
            }

            public GPSData(NavControlLibrary.Models.GPSData gps, GPSData previous)
            {
                time = gps.Time;

                JObject root = new JObject();
                root["time"] = time.ToString("yyyy-MM-dd HH:mm:ss");

                JObject position = new JObject();
                position["latitude"] = gps.Latitude;
                position["longitude"] = gps.Longitude;
                if (gps.Accuracy != null) position["accuracy"] = gps.Accuracy;
                root["position"] = position;

                //if (gps.Speed != 0.0)
                {
                    JObject speed = new JObject();
                    speed["value"] = gps.Speed;
                    if (gps.SpeedAccuracy != null) speed["accuracy"] = gps.SpeedAccuracy;
                    root["speed"] = speed;
                }

                if (gps.Bearing != null)
                {
                    JObject bearing = new JObject();
                    bearing["value"] = gps.Bearing;
                    if (gps.BearingAccuracy != null) bearing["accuracy"] = gps.BearingAccuracy;
                    root["bearing"] = bearing;
                }

                if (gps.Altitude != null)
                {
                    JObject altitude = new JObject();
                    altitude["value"] = gps.Altitude;
                    if (gps.AltitudeAccuracy != null) altitude["accuracy"] = gps.AltitudeAccuracy;
                    root["altitude"] = altitude;
                }

                data = root.ToString();
                init(previous);
            }


            public GPSData(MySqlDataReader location, GPSData previous)
            {
                time = location.GetDateTime("time");

                JObject root = new JObject();
                root["time"] = time.ToString("yyyy-MM-dd HH:mm:ss");

                JObject position = new JObject();
                position["latitude"] = location.GetDouble("latitude");
                position["longitude"] = location.GetDouble("longitude");
                if (!location.IsDBNull(5)) position["accuracy"] = location.GetFloat("accuracy");
                root["position"] = position;

                JObject speed = new JObject();
                speed["value"] = location.GetFloat("speed");
                if (!location.IsDBNull(7)) speed["accuracy"] = location.GetFloat("speedAccuracy");
                root["speed"] = speed;

                JObject bearing = new JObject();
                bearing["value"] = location.GetFloat("bearing");
                if (!location.IsDBNull(9)) bearing["accuracy"] = location.GetFloat("bearingAccuracy");
                root["bearing"] = bearing;

                if (!location.IsDBNull(10))
                {
                    JObject altitude = new JObject();
                    altitude["value"] = location.GetFloat("altitude");
                    if (!location.IsDBNull(11)) altitude["accuracy"] = location.GetDouble("altitudeAccuracy");
                    root["altitude"] = altitude;
                }

                data = root.ToString();
                init(previous);
            }
        }

        public NavControlLibrary.Map.MapModel mMapModel;
        string mFileName = "";
        List<GPSData> mData = new List<GPSData>();
        public static readonly List<String> mScaleList = new List<String> { "x1", "x2", "x5", "x10" };
        string mScale = "x1";
        ulong mPosition = 0;
        DispatcherTimer timer = new DispatcherTimer();

        public string FileName
        {
            get => mFileName;
        }
        public ulong LastSec
        {
            get
            {
                if (mData.Count == 0) return 0;
                else return mData.Last().sec;
            }
        }
        public List<string> ScaleList
        {
            get => mScaleList;
        }
        public string Scale
        {
            get => mScale;
            set
            {
                if (ScaleList.Contains(value))
                {
                    mScale = value;
                    NotifyPropertyChanged(nameof(Scale));
                    switch (mScale)
                    {
                        case "x2":
                            timer.Interval = TimeSpan.FromMilliseconds(500);
                            break;
                        case "x5":
                            timer.Interval = TimeSpan.FromMilliseconds(200);
                            break;
                        case "x10":
                            timer.Interval = TimeSpan.FromMilliseconds(100);
                            break;
                        default:
                            timer.Interval = TimeSpan.FromMilliseconds(1000);
                            break;
                    }
                }
            }
        }
        public ulong Position
        {
            get => mPosition;
            set
            {
                if (value > LastSec) value = LastSec;
                if (value != mPosition)
                {
                    mPosition = value;
                    var dt = mData.FindLast(x => x.sec < mPosition);
                    if (dt != null)
                    {
                        mMapModel.SetFromJSON(dt.data);
                        PrevPoint = dt.prev;
                    }
                    NotifyPropertyChanged("Position");
                }
            }
        }

        ulong mPrevPoint = 0;
        public ulong PrevPoint
        {
            get => mPrevPoint;
            set
            {
                mPrevPoint = value;
                NotifyPropertyChanged("PrevPoint");
            }
        }

        internal void Stop()
        {
            timer.Stop();
            NotifyPropertyChanged("IsPlayed");
        }

        internal void Play()
        {
            timer.Start();
            NotifyPropertyChanged("IsPlayed");
        }

        public bool IsPlayed
        {
            get => timer.IsEnabled;
        }
        public int RouteSize
        {
            get => mData.Count;
        }
        public string Begin
        {
            get
            {
                if (mData.Count > 0) return mData.First().time.ToString("HH:mm:ss dd.MM.yyyy");
                else return "---";
            }
        }
        public string End
        {
            get
            {
                if (mData.Count > 0) return mData.Last().time.ToString("HH:mm:ss dd.MM.yyyy");
                else return "---";
            }
        }

        public List<NavControlLibrary.Models.GPSData> GetRoute()
        {
            List<NavControlLibrary.Models.GPSData> rt = new List<NavControlLibrary.Models.GPSData>(mData.Count);
            foreach (var itm in mData)
            {
                rt.Add(new NavControlLibrary.Models.GPSData(itm.data));
            }
            return rt;
        }


        public Model()
        {
            mMapModel = new NavControlLibrary.Map.MapModel();
            timer.Interval = TimeSpan.FromMilliseconds(1000);
            timer.Tick += Timer_Tick;
        }

        internal void ClearRoute()
        {
            List<GPSData> data = new List<GPSData>();
            GPSData previous = null;
            NavControlLibrary.Models.GPSData g = null;
            foreach (GPSData itm in mData)
            {
                var g1 = new NavControlLibrary.Models.GPSData(itm.data);
                if ((g1.Speed < g1.SpeedAccuracy) || (g1.BearingAccuracy == null))
                {
                    g1.Speed = 0;
                }
                if (g == null)
                {
                    g = g1;
                }
                else
                {
                    //var tmp = new GPSData(g, previous);
                    //data.Add(tmp);
                    //previous = tmp;
                    //g = g1;

                    var d = g.getDistanceTo(g1);
                    var b = g.getBearing(g1);
                    var angle = g1.getDeltaBearing(b);

                    //if(g1.getDeltaTime(g) > 60.0)
                    //{
                    //    var tmp = new GPSData(g, previous);
                    //    data.Add(tmp);
                    //    previous = tmp;
                    //    g = g1;
                    //}
                    if (((g.Speed == 0.0) && (g1.Speed != 0.0)) || ((g1.Speed == 0.0) && (g.Speed != 0.0)))
                    {
                        var tmp = new GPSData(g, previous);
                        data.Add(tmp);
                        previous = tmp;
                        g = g1;
                    }
                    else if ((g.Speed == 0.0) && (g1.Speed == 0.0))
                    {
                        if (d > (g.Accuracy + g1.Accuracy))
                        {
                            var tmp = new GPSData(g, previous);
                            data.Add(tmp);
                            previous = tmp;
                            g = g1;
                        }
                    }
                    else if (angle < 5.0)
                    {
                    }
                    else if (g1.Accuracy < d)
                    {
                        if (g.Accuracy < d)
                        {
                            var tmp = new GPSData(g, previous);
                            data.Add(tmp);
                            previous = tmp;
                            g = g1;
                        }
                    }
                }
            }
            if (g != null) data.Add(new GPSData(g, previous));
            mData = data;

            NotifyPropertyChanged(nameof(LastSec));
            Position = 0;
            NotifyPropertyChanged(nameof(RouteSize));
            NotifyPropertyChanged(nameof(Begin));
            NotifyPropertyChanged(nameof(End));
        }

        private void Timer_Tick(object sender, EventArgs e)
        {
            Position++;
            if (Position == LastSec)
            {
                timer.Stop();
                NotifyPropertyChanged("IsPlayed");
            }
        }

        public void Open(string filename)
        {
            try
            {
                List<GPSData> data = new List<GPSData>();
                using (StreamReader rd = new StreamReader(filename))
                {
                    string line = rd.ReadLine();
                    GPSData previous = null;
                    while (line != null)
                    {
                        if (line.Contains("position"))
                        {
                            GPSData dt = new GPSData(line, previous);
                            previous = dt;
                            data.Add(dt);
                        }
                        line = rd.ReadLine();
                    }
                }
                mData = data;
                mFileName = filename;
                NotifyPropertyChanged(nameof(FileName));
                if (mData.Count != 0)
                {
                    mMapModel.SetFromJSON(mData.First().data);
                }
                NotifyPropertyChanged(nameof(LastSec));
                Position = 0;
                NotifyPropertyChanged(nameof(RouteSize));
                NotifyPropertyChanged(nameof(Begin));
                NotifyPropertyChanged(nameof(End));
            }
            catch
            {

            }
        }

        public void Open(string nm, MySqlDataReader rdr)
        {
            try
            {
                List<GPSData> data = new List<GPSData>();
                GPSData previous = null;
                while (rdr.Read())
                {
                    GPSData dt = new GPSData(rdr, previous);
                    previous = dt;
                    data.Add(dt);
                }

                mData = data;
                mFileName = nm;
                NotifyPropertyChanged(nameof(FileName));
                if (mData.Count != 0)
                {
                    mMapModel.SetFromJSON(mData.First().data);
                }
                NotifyPropertyChanged(nameof(LastSec));
                Position = 0;
                NotifyPropertyChanged(nameof(RouteSize));
                NotifyPropertyChanged(nameof(Begin));
                NotifyPropertyChanged(nameof(End));
            }
            catch
            {

            }
        }
    }
}

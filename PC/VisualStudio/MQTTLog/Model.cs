using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.ComponentModel;
using System.Linq;

namespace MQTTLog
{
    public class Station
    {
        public int id { get; set; }
        public string name { get; set; }
        public bool enable { get; set; }
    }

    public class Script
    {
        public int id = 0;
        [JsonIgnore]
        public string tag_id
        {
            get
            {
                return id.ToString();
            }
        }
        public bool menu { get; set; }
        public string name { get; set; }

        public Script()
        {
            name = "";
            menu = false;
        }

        public Script(Script s)
        {
            id = s.id;
            name = s.name;
            menu = s.menu;
        }

        public Script(Station s)
        {
            id = s.id;
            name = s.name;
            menu = true;
        }
    }

    public class Model : NavControlLibrary.NotifyModel
    {
        protected NavControlLibrary.MQTT.MQTTModel mMQTTModel;
        public NavControlLibrary.Map.MapModel mMapModel;

        string mMQTTConnection = "";

        string mInformerFile = "";
        bool mIsInformerRun = false;
        string mInformerRoute = "";
        int mStationIndex = -1;
        string mInformerDirrection = "?";

        string mRouteZip = "https://romasty.duckdns.org/short.zip";
        string mStartFile = "short/short.jsonc";
        string mStartPayload = "{\"GPS\":{\"run\":\"on\",\"interval\":1500,\"fastestInterval\":1000,\"minInterval\":3000,\"dutyCycle\":10}}";

        public string StartPayload
        {
            get
            {
                return mStartPayload;
            }
            set
            {
                mStartPayload = value;
                NotifyPropertyChanged("StartPayload");
            }
        }

        public string StartFile
        {
            get
            {
                return mStartFile;
            }
            set
            {
                mStartFile = value;
                NotifyPropertyChanged("StartFile");
            }
        }

        public string RouteZip
        {
            get
            {
                return mRouteZip;
            }
            set
            {
                mRouteZip = value;
                NotifyPropertyChanged("RouteZip");
            }
        }


        string mDisplay1 = "";
        string mDisplay2 = "";
        string mDisplay3 = "";
        string mDisplay4 = "";

        public string InformerDirrection
        {
            get
            {
                return mInformerDirrection;
            }
            set
            {
                mInformerDirrection = value;
                NotifyPropertyChanged("InformerDirrection");
            }
        }

        public string Display1
        {
            get
            {
                return mDisplay1;
            }
            set
            {
                mDisplay1 = value;
                NotifyPropertyChanged("Display1");
            }
        }
        public string Display2
        {
            get
            {
                return mDisplay2;
            }
            set
            {
                mDisplay2 = value;
                NotifyPropertyChanged("Display2");
            }
        }
        public string Display3
        {
            get
            {
                return mDisplay3;
            }
            set
            {
                mDisplay3 = value;
                NotifyPropertyChanged("Display3");
            }
        }
        public string Display4
        {
            get
            {
                return mDisplay4;
            }
            set
            {
                mDisplay4 = value;
                NotifyPropertyChanged("Display4");
            }
        }

        public string InformerRoute
        {
            get
            {
                return mInformerRoute;
            }
            set
            {
                mInformerRoute = value;
                NotifyPropertyChanged("InformerRoute");
            }
        }

        public BindingList<Station> Stations { get; }
        public BindingList<Script> Scripts { get; }
        public BindingList<Script> AllScripts { get; }

        public int StationIndex
        {
            get
            {
                return mStationIndex;
            }
            set
            {
                mStationIndex = value;
                NotifyPropertyChanged("StationIndex");
            }
        }

        public string InformerFile
        {
            get
            {
                return mInformerFile;
            }
            set
            {
                mInformerFile = value;
                NotifyPropertyChanged("InformerFile");
            }
        }

        public string MQTTConnection
        {
            get
            {
                return mMQTTConnection;
            }
            set
            {
                mMQTTConnection = value;
                NotifyPropertyChanged("MQTTConnection");
            }
        }

        public bool IsInformerRun
        {
            get
            {
                return mIsInformerRun;
            }
            set
            {
                mIsInformerRun = value;
                NotifyPropertyChanged("IsInformerRun");
            }
        }

        public NavControlLibrary.MQTT.MQTTModel MQTTModel
        {
            get
            {
                return mMQTTModel;
            }
            set
            {
                mMQTTModel = value;
                mMQTTModel.onReceive += MMQTTModel_onReceive;
            }
        }

        public Model()
        {
            Stations = new BindingList<Station>();
            Scripts = new BindingList<Script>();
            AllScripts = new BindingList<Script>();
            mMQTTModel = new NavControlLibrary.MQTT.MQTTModel();
            mMapModel = new NavControlLibrary.Map.MapModel();
        }


        private delegate void ponReceive(object sender, string topic, string msg, DateTime time);
        private void MMQTTModel_onReceive(object sender, string topic, string msg, DateTime time)
        {
            if (!System.Windows.Application.Current.Dispatcher.CheckAccess())
            {
                ponReceive d = new ponReceive(MMQTTModel_onReceive);
                System.Windows.Application.Current.Dispatcher.Invoke(d, new object[] { sender, topic, msg, time });
            }
            else
            {
                if (topic == "GPS") mMapModel.SetFromJSON(msg);
                if (topic == "services/MQTT")
                {
                    if (msg != null)
                    {
                        var x = JObject.Parse(msg);
                        if ((string)x["state"] == "RUN") MQTTConnection = x["connection"].ToString(Newtonsoft.Json.Formatting.Indented).Trim('{', '}').Trim();
                        else MQTTConnection = "";
                    }
                    else MQTTConnection = "";
                }
                if (topic == "services/Informer")
                {
                    if (msg != null)
                    {
                        var x = JObject.Parse(msg);
                        if ((string)x["state"] == "RUN")
                        {
                            InformerFile = (string)x["connection"]["file"];
                            IsInformerRun = true;
                        }
                        else IsInformerRun = false;
                    }
                    else IsInformerRun = false;
                }
                if (topic == "Informer")
                {
                    if (msg != null)
                    {
                        var x = JObject.Parse(msg);
                        if (x["route"] != null)
                        {
                            int? tmp = null;
                            if (StationIndex >= 0)
                            {
                                tmp = Stations[StationIndex].id;
                            }
                            StationIndex = -1;
                            Stations.Clear();
                            foreach (var itm in x["route"].Children())
                            {
                                Stations.Add(JsonConvert.DeserializeObject<Station>(itm.ToString()));
                            }
                            if (tmp != null)
                            {
                                for (int i = 0; i < Stations.Count; i++)
                                {
                                    if (Stations[i].id == tmp)
                                    {
                                        StationIndex = i;
                                        break;
                                    }
                                }
                            }
                            CreateAllScript();
                        }
                        if (x["scripts"] != null)
                        {
                            Scripts.Clear();
                            foreach (var itm in x["scripts"].Children())
                            {
                                Scripts.Add(JsonConvert.DeserializeObject<Script>(itm.ToString()));
                            }
                            CreateAllScript();
                        }
                        if (x["station"] != null)
                        {
                            int st = (int)x["station"];
                            for (int i = 0; i < Stations.Count; i++)
                            {
                                if (Stations[i].id == st)
                                {
                                    StationIndex = i;
                                    break;
                                }
                            }
                        }
                        if (x["name"] != null)
                        {
                            InformerRoute = (string)x["name"];
                        }
                        if (x["display"] != null)
                        {
                            var lst = x["display"]["id"].Children().Values<int>();
                            if ((lst.Count() == 0) || lst.Contains(1))
                            {
                                Display1 = (string)x["display"]["text"];
                            }
                            if (lst.Contains(2))
                            {
                                Display2 = (string)x["display"]["text"];
                            }
                            if (lst.Contains(3))
                            {
                                Display3 = (string)x["display"]["text"];
                            }
                            if (lst.Contains(4))
                            {
                                Display4 = (string)x["display"]["text"];
                            }
                        }
                        if (x["forward"] != null)
                        {
                            if ((bool)x["forward"]) InformerDirrection = "Forward";
                            else InformerDirrection = "Backward";
                        }
                    }
                }
            }
        }

        void CreateAllScript()
        {
            AllScripts.Clear();
            foreach (var itm in Scripts)
            {
                AllScripts.Add(new Script(itm));
            }
            foreach (var itm in Stations)
            {
                AllScripts.Add(new Script(itm));
            }
        }

        public void StopMQTTService()
        {
            mMQTTModel.Send("cmd", "{\"service\":{\"MQTT\":{\"run\":\"off\"}}}");
        }

        public void ToggleInformerService()
        {
            if (IsInformerRun)
            {
                mMQTTModel.Send("cmd", "{\"service\":{\"Informer\":{\"run\":\"off\"}}}");
            }
            else
            {
                mMQTTModel.Send("cmd", "{\"service\":{\"Informer\":{\"run\":\"on\",\"connection\":{\"file\":\"" +
                    InformerFile +
                    "\"},\"payload\":{\"GPS\":{\"run\":\"on\",\"interval\":10000,\"fastestInterval\":1000,\"minInterval\":3000,\"dutyCycle\":10}}}}}");
            }
        }

        public void SendToInformer(string json)
        {
            if (IsInformerRun)
            {
                mMQTTModel.Send("cmd/Informer", json);
            }
        }

        public void SendToMQTT(string json)
        {
            mMQTTModel.Send("cmd/MQTT", json);
        }

        public void SendCommand(string json)
        {
            mMQTTModel.Send("cmd", json);
        }
    }
}

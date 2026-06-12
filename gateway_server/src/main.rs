mod balancer;

use anyhow::{Context, Result};
use balancer::Balancer;
use lab7_shared::{read_env, read_frame, write_frame, Request};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, Mutex};
use std::thread;

fn handle_client(balancer: Arc<Mutex<Balancer>>, mut stream: TcpStream) -> Result<()> {
    let request: Request = read_frame(&mut stream)?;
    let response = balancer.lock().unwrap().handle(request);
    write_frame(&mut stream, &response)
}

fn main() -> Result<()> {
    let env = read_env(".env")?;
    let host = env
        .get("GW_HOST")
        .context("check for GW_HOST in .env")?
        .clone();
    let port: u16 = env
        .get("GW_PORT")
        .context("check for GW_PORT in .env")?
        .parse()?;
    let listener = TcpListener::bind((host.as_str(), port))?;
    let balancer = Arc::new(Mutex::new(Balancer::default()));

    println!("Gateway started at {host}:{port}");
    for stream in listener.incoming() {
        match stream {
            Ok(stream) => {
                let balancer = balancer.clone();
                thread::spawn(move || {
                    if let Err(err) = handle_client(balancer, stream) {
                        eprintln!("{err:?}");
                    }
                });
            }
            Err(err) => eprintln!("{err:?}"),
        }
    }
    Ok(())
}
